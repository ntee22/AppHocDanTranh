//
//  TunerView.swift
//  Dan Thanh Guzheng
//
//  Rewritten to use Tuna + YIN algorithm for stable pitch detection.
//

import SwiftUI
import Combine
import UIKit
import Tuna
import AVFoundation

// MARK: - Tuning Data

struct GString: Identifiable {
    let id: Int
    let note: String
    let label: String
    let frequency: Double
    let isGreen: Bool
}

let GUZHENG_STRINGS: [GString] = [
    GString(id: 1,  note: "D",  label: "D3",                         frequency: 1172.5, isGreen: false),
    GString(id: 2,  note: "B",  label: "B2",                         frequency: 985.5,  isGreen: false),
    GString(id: 3,  note: "A",  label: "A2",                         frequency: 889.2,  isGreen: true),
    GString(id: 4,  note: "F♯", label: "F♯2",                        frequency: 737.6,  isGreen: false),
    GString(id: 5,  note: "E",  label: "E2",                         frequency: 662.8,  isGreen: false),
    GString(id: 6,  note: "D",  label: "D2",                         frequency: 585.4,  isGreen: false),
    GString(id: 7,  note: "B",  label: "B1",                         frequency: 489.5,  isGreen: false),
    GString(id: 8,  note: "A",  label: "A1",                         frequency: 437.9,  isGreen: true),
    GString(id: 9,  note: "F♯", label: "F♯1",                        frequency: 368.1,  isGreen: false),
    GString(id: 10, note: "E",  label: "E1",                         frequency: 330.4,  isGreen: false),
    GString(id: 11, note: "D",  label: "D",                          frequency: 293.8,  isGreen: false),
    GString(id: 12, note: "B",  label: "B",                          frequency: 246.2,  isGreen: false),
    GString(id: 13, note: "A",  label: "A",                          frequency: 218.4,  isGreen: true),
    GString(id: 14, note: "F♯", label: "F♯",                         frequency: 184.7,  isGreen: false),
    GString(id: 15, note: "E",  label: "E",                          frequency: 164.4,  isGreen: false),
    GString(id: 16, note: "D",  label: "D\u{0332}",                  frequency: 146.2,  isGreen: false),
    GString(id: 17, note: "B",  label: "B\u{0332}",                  frequency: 123.0,  isGreen: false),
    GString(id: 18, note: "A",  label: "A\u{0332}",                  frequency: 109.5,  isGreen: true),
    GString(id: 19, note: "F♯", label: "F\u{0332}♯",                 frequency: 92.3,   isGreen: false),
    GString(id: 20, note: "E",  label: "E\u{0332}",                  frequency: 81.9,   isGreen: false),
    GString(id: 21, note: "D",  label: "D\u{0332}\u{0332}",          frequency: 73.1,   isGreen: false),
]

// MARK: - TunerState

enum TunerState: Equatable {
    case listening
    case matched(stringID: Int, until: Date)
    case waitingForSilence(silenceStartedAt: Date?)
}

// MARK: - TunerTuning

private enum TunerTuning {
    static let silenceDuration: TimeInterval = 0.3   // seconds of quiet to leave the "matched" cooldown
    static let matchCooldown: TimeInterval = 1.5     // how long to stay in `.matched` after a confirmed match
    static let pitchToleranceCents: Double = 5.0     // ± cents counted as "in tune" for match-lock
    static let silenceThreshold: Float = -35.0       // (legacy, retained for state-machine compile)

    // Stability frames required to confirm a match, per sensitivity tier.
    // Higher tier = fewer frames = quicker lock = more responsive but less stable.
    static func stabilityFramesRequired(for sensitivity: String) -> Int {
        switch sensitivity {
        case "low":  return 7   // slower lock, more stable
        case "high": return 3   // quicker lock, more responsive
        default:     return 5   // balanced
        }
    }
}

// MARK: - TunerEngine

class TunerEngine: ObservableObject {
    @Published var activeStringID: Int = 8
    @Published var detectedHz: Double = 0
    @Published var cents: Double = 0
    @Published var isInTune: Bool = false
    @Published var isSounding: Bool = false
    @Published var manualLock: Bool = false
    @Published private(set) var tunerState: TunerState = .listening
    @Published private(set) var isPlayingReference: Bool = false

    var sensitivity: String = "medium"

    private var pitchEngine: PitchEngine?
    private var referencePlayer: AVAudioPlayer?
    private var playbackEndWorkItem: DispatchWorkItem?
    private var pendingID: Int = 8
    private var pendingCount: Int = 0
    private let confirmFrames = 3
    private var lastConfirmedID: Int = 8
    private var consecutiveMatchCount: Int = 0

    func start() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: .duckOthers)
            try session.setActive(true)
        } catch {
            print("TunerEngine: audio session setup failed: \(error)")
            return
        }

        pitchEngine = PitchEngine(
            bufferSize: 4096,
            estimationStrategy: .yin
        ) { [weak self] result in
            DispatchQueue.main.async {
                let level = self?.pitchEngine?.signalLevel ?? -160
                switch result {
                case .success(let pitch):
                    self?.processAudioFrame(frequency: pitch.frequency, level: level)
                case .failure:
                    self?.processAudioFrame(frequency: nil, level: level)
                }
            }
        }
        DispatchQueue.global(qos: .userInitiated).async {
            self.pitchEngine?.start()
        }
    }

    func stop() {
        stopReferencePlayback()
        pitchEngine?.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    func selectString(_ id: Int) {
        // Second tap on the already-locked string toggles back to auto-detect.
        if manualLock && id == activeStringID {
            manualLock = false
            stopReferencePlayback()
            tunerState = .listening
            consecutiveMatchCount = 0
            return
        }

        activeStringID = id
        manualLock = true
        pendingID = id
        pendingCount = confirmFrames
        tunerState = .listening
        consecutiveMatchCount = 0
        playReferenceSound(for: id)
    }

    func clearManualLock() {
        manualLock = false
    }

    // MARK: - Reference playback

    private func playReferenceSound(for id: Int) {
        let resourceName = String(format: "string_%02d", id)
        guard let url = Bundle.main.url(forResource: resourceName, withExtension: "m4a") else {
            print("TunerEngine: reference audio \(resourceName).m4a not found in bundle")
            return
        }

        // Interrupt any in-flight playback before starting the next one.
        referencePlayer?.stop()
        referencePlayer = nil

        // Don't stop the pitch engine — restarting it leaves AVCaptureSession in a bad
        // state and the tuner stops detecting. Instead, gate audio frames in
        // `processAudioFrame` while `isPlayingReference == true`.
        isSounding = false

        // Defensive: Tuna normally upgrades the category to .playAndRecord on its async
        // start, but if the user taps a string before that completes, the session is
        // still .record and AVAudioPlayer.play() would silently fail. Upgrade explicitly.
        let session = AVAudioSession.sharedInstance()
        if session.category != .playAndRecord {
            try? session.setCategory(.playAndRecord, options: [.duckOthers, .defaultToSpeaker])
            try? session.overrideOutputAudioPort(.speaker)
        }

        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.prepareToPlay()
            referencePlayer = player
            isPlayingReference = true
            player.play()

            // Use a timer rather than AVAudioPlayerDelegate so the engine stays a
            // plain ObservableObject — no NSObject inheritance, no MainActor /
            // delegate isolation gymnastics that can interfere with Tuna's callback.
            let duration = player.duration
            playbackEndWorkItem?.cancel()
            let work = DispatchWorkItem { [weak self] in
                self?.handlePlaybackFinished()
            }
            playbackEndWorkItem = work
            DispatchQueue.main.asyncAfter(deadline: .now() + duration + 0.1, execute: work)
        } catch {
            print("TunerEngine: failed to load reference audio: \(error)")
            isPlayingReference = false
        }
    }

    private func stopReferencePlayback() {
        playbackEndWorkItem?.cancel()
        playbackEndWorkItem = nil
        guard isPlayingReference || referencePlayer != nil else { return }
        referencePlayer?.stop()
        referencePlayer = nil
        isPlayingReference = false
    }

    private func handlePlaybackFinished() {
        isPlayingReference = false
        referencePlayer = nil
        playbackEndWorkItem = nil
        // Engine kept running throughout — nothing to restart.
    }

    // MARK: - State machine

    private func processAudioFrame(frequency: Double?, level: Float) {
        if isPlayingReference {
            isSounding = false
            consecutiveMatchCount = 0
            return
        }

        switch tunerState {
        case .matched(_, let until):
            // Stay locked on a confirmed string until the cooldown elapses.
            // No level-based silence gate — Tuna's signalLevel is unreliable here.
            // The stability counter in handlePitch already prevents re-triggering.
            if Date() >= until {
                tunerState = .listening
                consecutiveMatchCount = 0
            }
            return

        case .waitingForSilence:
            // Legacy state — treat as listening. Will be removed.
            tunerState = .listening
            consecutiveMatchCount = 0
            fallthrough

        case .listening:
            // No amplitude gate — `level` from Tuna's signalLevel reads from
            // AVCaptureAudioChannel.averagePowerLevel which is unreliable on iOS 26
            // (often stuck at -160). Trust Tuna's pitch result instead: success means
            // there was enough signal to extract a pitch; failure means no clear pitch.
            guard let hz = frequency else {
                isSounding = false
                consecutiveMatchCount = 0
                return
            }
            handlePitch(hz: hz)
        }
    }

    private func handlePitch(hz: Double) {
        // Gate: reject frequencies outside Guzheng's playable range
        guard hz >= 55 && hz <= 1500 else { return }

        isSounding = true
        detectedHz = hz

        guard let closest = GUZHENG_STRINGS.min(by: {
            abs($0.frequency - hz) < abs($1.frequency - hz)
        }) else { return }

        let targetHz = manualLock
            ? (GUZHENG_STRINGS.first { $0.id == activeStringID }?.frequency ?? hz)
            : closest.frequency

        let centsValue = 1200 * log2(hz / targetHz)

        // Reject readings more than 2 semitones off — likely a harmonic glitch
        guard abs(centsValue) <= 200 else { return }

        cents = centsValue
        isInTune = abs(centsValue) < 15

        // Stability check: only fire the match feedback after several consecutive in-tolerance frames.
        // The required frame count is driven by user-selected sensitivity in Settings.
        let stringID = manualLock ? activeStringID : closest.id
        let stabilityFrames = TunerTuning.stabilityFramesRequired(for: sensitivity)
        if abs(centsValue) <= TunerTuning.pitchToleranceCents {
            consecutiveMatchCount += 1
            if consecutiveMatchCount >= stabilityFrames {
                triggerMatchFeedback()
                tunerState = .matched(
                    stringID: stringID,
                    until: Date().addingTimeInterval(TunerTuning.matchCooldown)
                )
                consecutiveMatchCount = 0
                ReviewManager.shared.recordPositiveAction()
                return
            }
        } else {
            consecutiveMatchCount = 0
        }

        if manualLock { return }

        if closest.id == pendingID {
            pendingCount += 1
            if pendingCount >= confirmFrames {
                activeStringID = closest.id
                lastConfirmedID = closest.id
            }
        } else {
            pendingID = closest.id
            pendingCount = 1
        }
    }

    private func triggerMatchFeedback() {
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        AudioServicesPlaySystemSound(1407)
    }
}

// MARK: - StringRow

struct StringRow: View {
    let string: GString
    let isActive: Bool
    let onTap: () -> Void

    @State private var breathScale: CGFloat = 1.0
    @State private var breathOpacity: Double = 0.3

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                Text("\(string.id)")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(
                        isActive
                            ? Color(hex: "F2E8D5")
                            : Color(hex: "F2E8D5").opacity(0.5)
                    )
                    .frame(width: 24, alignment: .trailing)

                ZStack {
                    if isActive {
                        Rectangle()
                            .fill(string.isGreen
                                ? Color(hex: "8FBF7A")
                                : Color(hex: "F2E8D5"))
                            .frame(height: string.isGreen ? 4 : 3)
                            .blur(radius: 4)
                            .opacity(breathOpacity)
                            .scaleEffect(x: breathScale)
                    }
                    Rectangle()
                        .fill(string.isGreen
                            ? Color(hex: "8FBF7A")
                            : (isActive
                                ? Color(hex: "FFFFFF")
                                : Color(hex: "F2E8D5").opacity(0.7)))
                        .frame(height: string.isGreen ? 2.5 : 1.8)
                }
                .frame(maxWidth: .infinity)

                Text(string.label)
                    .font(.custom("Cormorant Garamond", size: 12))
                    .italic()
                    .foregroundColor(
                        isActive
                            ? Color(hex: "F2E8D5")
                            : Color(hex: "F2E8D5").opacity(0.5)
                    )
                    .frame(width: 24, alignment: .leading)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
            .padding(.horizontal, 20)
            .background(
                isActive
                    ? Color(hex: "6B7C5E").opacity(0.2)
                    : Color.clear
            )
            .cornerRadius(4)
        }
        .buttonStyle(PlainButtonStyle())
        .onChange(of: isActive) { _, active in
            if active {
                withAnimation(
                    .easeInOut(duration: 1.4)
                    .repeatForever(autoreverses: true)
                ) {
                    breathScale = 1.05
                    breathOpacity = 0.7
                }
            } else {
                withAnimation(.easeOut(duration: 0.3)) {
                    breathScale = 1.0
                    breathOpacity = 0.3
                }
            }
        }
    }
}

// MARK: - CentsMeter

struct CentsMeter: View {
    let cents: Double
    let isInTune: Bool
    let isSounding: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 6)
                .fill(Color(hex: "EFE0C7").opacity(0.7))
                .frame(height: 48)

            Rectangle()
                .fill(Color(hex: "6B7C5E").opacity(0.12))
                .frame(width: 20, height: 48)

            ForEach([-40, -20, 0, 20, 40], id: \.self) { v in
                Rectangle()
                    .fill(v == 0
                        ? Color(hex: "6B7C5E")
                        : Color(hex: "8A7B66").opacity(0.3))
                    .frame(width: v == 0 ? 2 : 1, height: 36)
                    .offset(x: CGFloat(v) * 1.8)
            }

            if isSounding {
                Rectangle()
                    .fill(isInTune
                        ? Color(hex: "6B7C5E")
                        : Color(hex: "B84A3E"))
                    .frame(width: 3, height: 38)
                    .cornerRadius(1.5)
                    .offset(x: CGFloat(max(-40, min(40, cents))) * 1.8)
                    .shadow(
                        color: isInTune
                            ? Color(hex: "6B7C5E").opacity(0.6)
                            : Color(hex: "B84A3E").opacity(0.5),
                        radius: 4
                    )
                    .animation(
                        .spring(response: 0.15, dampingFraction: 0.7),
                        value: cents
                    )
            }

            HStack {
                Text("♭ thấp")
                Spacer()
                Text("♯ cao")
            }
            .font(.system(size: 9, weight: .semibold))
            .foregroundColor(Color(hex: "8A7B66"))
            .padding(.horizontal, 10)
            .offset(y: 14)
        }
        .opacity(isSounding ? 1.0 : 0.45)
    }
}

// MARK: - TunerView

struct TunerView: View {
    @StateObject private var tuner = TunerEngine()
    @AppStorage("appLanguage") private var appLanguage: String = "vi"
    @AppStorage("tunerSensitivity") private var tunerSensitivity: String = "medium"
    @AppStorage("dt-tuner-guide-seen") private var guideSeen: String = ""
    @State private var showGuide = false
    private var isEnglish: Bool { appLanguage == "en" }

    var activeString: GString {
        GUZHENG_STRINGS.first { $0.id == tuner.activeStringID } ?? GUZHENG_STRINGS[7]
    }

    private var isMatched: Bool {
        if case .matched = tuner.tunerState { return true }
        return false
    }

    private var isWaitingForSilence: Bool {
        if case .waitingForSilence = tuner.tunerState { return true }
        return false
    }

    var statusText: String {
        if tuner.isPlayingReference {
            return isEnglish ? "♪ Reference playing..." : "♪ Đang phát mẫu..."
        }
        if isMatched {
            return isEnglish ? "✓ Locked!" : "✓ Đã đúng!"
        }
        if isWaitingForSilence {
            return isEnglish ? "Pause, then pluck next..." : "Đợi một chút rồi gảy dây tiếp..."
        }
        guard tuner.isSounding else { return "" }
        if tuner.isInTune { return isEnglish ? "✅ In tune!" : "✅ Đúng rồi!" }
        return tuner.cents < 0
            ? (isEnglish ? "🔼 Tighten +\(abs(Int(tuner.cents)))¢" : "🔼 Căng thêm +\(abs(Int(tuner.cents)))¢")
            : (isEnglish ? "🔽 Loosen -\(Int(tuner.cents))¢" : "🔽 Nới bớt -\(Int(tuner.cents))¢")
    }

    var statusColor: Color {
        if tuner.isPlayingReference { return Color.sageDark }
        if isMatched { return Color.sageDark }
        if isWaitingForSilence { return Color.inkMuted }
        guard tuner.isSounding else { return Color.inkMuted }
        return tuner.isInTune ? Color.sageDark : Color.sealRed
    }

    var body: some View {
        ZStack {
            PaperBackground()

            VStack(spacing: 0) {

                // ── HEADER ──
                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 2) {
                        OverlineLabel(text: "調音 · Tuner")
                        HStack(spacing: 4) {
                            Text(isEnglish ? "Tune" : "Lên dây")
                                .font(.system(size: 32, weight: .medium, design: .serif))
                                .foregroundColor(Color.inkDark)
                            Text("Đàn Tranh")
                                .font(.system(size: 32, weight: .light, design: .serif))
                                .italic()
                                .foregroundColor(Color.sageDark)
                        }
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 5) {
                        HStack(spacing: 8) {
                            helpButton
                            Text("Tone D")
                                .font(.system(size: 15, weight: .semibold, design: .serif))
                                .italic()
                                .foregroundColor(Color.sageDark)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color.sageDark.opacity(0.12))
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(Color.sageDark.opacity(0.3), lineWidth: 1))
                        }
                        HStack(spacing: 5) {
                            Circle()
                                .fill(tuner.isSounding ? Color.sageDark : Color.inkMuted.opacity(0.3))
                                .frame(width: 6, height: 6)
                                .animation(.easeInOut(duration: 0.3), value: tuner.isSounding)
                            Text(tuner.isSounding
                                 ? (isEnglish ? "🎙️ Listening" : "🎙️ Đang nhận âm")
                                 : (isEnglish ? "👂 Waiting..." : "👂 Lắng nghe..."))
                                .font(.system(size: 10))
                                .foregroundColor(Color.inkMuted)
                        }
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 56)
                .padding(.bottom, 10)

                // ── ACTIVE STRING CARD ──
                VStack(spacing: 4) {
                    HStack(alignment: .center) {
                        (Text("Dây ")
                            .font(.system(size: 28, weight: .light, design: .serif))
                            .foregroundColor(Color.inkDark)
                        + Text("\(activeString.id)")
                            .font(.system(size: 28, weight: .medium, design: .serif))
                            .italic()
                        )
                        .foregroundColor(
                            tuner.isInTune && tuner.isSounding ? Color.sageDark : Color.inkDark
                        )
                        .animation(.easeInOut(duration: 0.3), value: tuner.isInTune)

                        Text(activeString.label)
                            .font(.system(size: 15, weight: .medium, design: .serif))
                            .italic()
                            .foregroundColor(Color.inkSoft)
                            .padding(.leading, 6)

                        Text(String(format: "%.1f Hz", activeString.frequency))
                            .font(.system(size: 11))
                            .foregroundColor(Color.inkMuted.opacity(0.7))
                            .padding(.leading, 2)

                        Spacer()

                        Text(statusText)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(statusColor)
                            .multilineTextAlignment(.trailing)
                    }

                    centMeter
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 10)
                .background(
                    LinearGradient(
                        colors: [Color.paperWarm.opacity(0.9), Color.paperDeep.opacity(0.85)],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(Color.inkMuted.opacity(0.22), lineWidth: 1)
                )
                .padding(.horizontal, 24)
                .padding(.bottom, 10)

                // ── STRING LIST HEADER ──
                HStack {
                    OverlineLabel(text: isEnglish ? "Strings · 21 strings" : "Dây · 21 strings")
                    Spacer()
                    Text(isEnglish ? "High ↑" : "Cao ↑")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(Color.inkMuted.opacity(0.7))
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 6)

                // ── STRING LIST ──
                ScrollView {
                    VStack(spacing: 1) {
                        ForEach(GUZHENG_STRINGS.reversed()) { string in
                            StringRow(
                                string: string,
                                isActive: tuner.activeStringID == string.id,
                                onTap: { tuner.selectString(string.id) }
                            )
                        }
                    }
                    .padding(.vertical, 8)
                }
                .frame(maxHeight: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(LinearGradient(
                            colors: [Color.woodDark, Color.wood.opacity(0.85), Color.woodDark],
                            startPoint: .leading, endPoint: .trailing
                        ))
                        .shadow(color: Color.inkDark.opacity(0.2), radius: 6, x: 0, y: 2)
                )
                .padding(.horizontal, 24)

                // ── LEGEND ──
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Color(hex: "8FBF7A"))
                        .frame(width: 18, height: 2.5)
                        .shadow(color: Color(hex: "8FBF7A").opacity(0.5), radius: 2)
                    Text(isEnglish ? "String La (5/A) — green reference string 💚" : "Dây La (5/A) — dây xanh định vị 💚")
                        .font(.system(size: 10))
                        .foregroundColor(Color.inkMuted)
                }
                .padding(.horizontal, 24)
                .padding(.top, 6)
                .padding(.bottom, 10)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .blur(radius: showGuide ? 6 : 0)
            .animation(.easeInOut(duration: 0.28), value: showGuide)

            // ── FIRST-TIME TUTORIAL POPUP ──
            if showGuide {
                TunerGuidePopup(
                    onStart:    { dismissGuide(persist: false) },
                    onNeverShow: { dismissGuide(persist: true) },
                    onBackdrop: { dismissGuide(persist: false) }
                )
                .transition(.opacity)
                .zIndex(1)
            }
        }
        .onAppear {
            tuner.sensitivity = tunerSensitivity
            tuner.start()
            if guideSeen != "1" {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    withAnimation(.easeInOut(duration: 0.28)) { showGuide = true }
                }
            }
        }
        .onChange(of: tunerSensitivity) { _, newValue in
            tuner.sensitivity = newValue
        }
        .onDisappear { tuner.stop() }
    }

    // Reuses the existing CentsMeter but wired to app color palette
    private var centMeter: some View {
        CentsMeter(
            cents: tuner.cents,
            isInTune: tuner.isInTune,
            isSounding: tuner.isSounding
        )
    }

    // "?" header button — reopens the tutorial popup anytime
    private var helpButton: some View {
        Button { withAnimation(.easeInOut(duration: 0.28)) { showGuide = true } } label: {
            Text("?")
                .font(.system(size: 16, weight: .semibold, design: .serif))
                .italic()
                .foregroundColor(Color.sageDark)
                .frame(width: 32, height: 32)
                .background(Color.sageDark.opacity(0.12))
                .clipShape(Circle())
                .overlay(Circle().stroke(Color.sageDark.opacity(0.3), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func dismissGuide(persist: Bool) {
        if persist { guideSeen = "1" }
        withAnimation(.easeInOut(duration: 0.25)) { showGuide = false }
    }
}

// MARK: - TunerGuidePopup

/// First-time tutorial popup for the Tuner. Auto-shows on first visit (gated by
/// the `dt-tuner-guide-seen` UserDefaults key) and re-openable via the header "?" button.
struct TunerGuidePopup: View {
    let onStart: () -> Void
    let onNeverShow: () -> Void
    let onBackdrop: () -> Void

    @State private var appear = false

    private struct Step: Identifiable {
        let id = UUID()
        let icon: String
        let title: String
        let body: String
    }

    private let steps: [Step] = [
        Step(icon: "🎧", title: "Tự động lắng nghe",
             body: "Mặc định app luôn nghe. Đánh vào dây thật, app sẽ tự nhận diện cao độ."),
        Step(icon: "↕", title: "Căng thêm hay nới bớt",
             body: "Nhìn kim trên thước: lệch phải là cao (♯ nới bớt), lệch trái là thấp (♭ căng thêm). Vào giữa, xanh là đúng."),
        Step(icon: "☝", title: "Chỉnh riêng một dây",
             body: "Bấm vào một dây bất kỳ để khoá nghe riêng dây đó."),
        Step(icon: "↺", title: "Quay về tự động",
             body: "Bấm lại đúng dây đang chọn để trở về chế độ nghe tự động."),
    ]

    var body: some View {
        ZStack {
            // Dimmed backdrop — tap to dismiss without persisting
            Color(hex: "2A2520").opacity(0.55)
                .ignoresSafeArea()
                .onTapGesture { onBackdrop() }

            card
                .frame(maxWidth: 320)
                .padding(24)
                .rotation3DEffect(
                    .degrees(appear ? 0 : 78),
                    axis: (x: 1, y: 0, z: 0),
                    anchor: .center,
                    perspective: 0.6
                )
                .opacity(appear ? 1 : 0)
        }
        .onAppear {
            withAnimation(.timingCurve(0.22, 0.9, 0.35, 1, duration: 0.42)) {
                appear = true
            }
        }
    }

    private var card: some View {
        VStack(spacing: 0) {
            // Header
            VStack(spacing: 2) {
                InkSeal(char: "調", size: 42, rotate: -3)
                    .padding(.bottom, 10)
                Text("Cách lên dây")
                    .font(.system(size: 24, weight: .medium, design: .serif))
                    .foregroundColor(Color.inkDark)
                Text("How to tune your Đàn Tranh")
                    .font(.system(size: 14, design: .serif))
                    .italic()
                    .foregroundColor(Color.inkMuted)
            }
            .padding(.bottom, 16)

            // Numbered steps
            VStack(spacing: 0) {
                ForEach(Array(steps.enumerated()), id: \.element.id) { idx, step in
                    HStack(alignment: .top, spacing: 12) {
                        Text(step.icon)
                            .font(.system(size: 14))
                            .foregroundColor(Color.sageDark)
                            .frame(width: 30, height: 30)
                            .background(Color.sageDark.opacity(0.15))
                            .clipShape(Circle())
                            .overlay(Circle().stroke(Color.sageDark.opacity(0.3), lineWidth: 1))
                        VStack(alignment: .leading, spacing: 1) {
                            Text(step.title)
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(Color.inkDark)
                            Text(step.body)
                                .font(.system(size: 12))
                                .foregroundColor(Color.inkSoft)
                                .lineSpacing(3)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(.vertical, 9)
                    .overlay(alignment: .bottom) {
                        if idx < steps.count - 1 {
                            Rectangle()
                                .fill(Color.woodDark.opacity(0.12))
                                .frame(height: 1)
                        }
                    }
                }
            }
            .padding(.bottom, 6)

            // Primary CTA — closes without persisting
            Button(action: onStart) {
                Text("BẮT ĐẦU LÊN DÂY")
                    .font(.system(size: 13, weight: .bold))
                    .kerning(1)
                    .foregroundColor(Color.paper)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(Color.sageDark)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .padding(.top, 16)

            // Secondary — closes AND sets the "seen" flag
            Button(action: onNeverShow) {
                Text("Không hiện lần sau")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Color.inkMuted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
            .buttonStyle(.plain)
            .padding(.top, 8)
        }
        .padding(EdgeInsets(top: 22, leading: 22, bottom: 18, trailing: 22))
        .background(
            LinearGradient(
                colors: [Color.paperWarm, Color.paperDeep.opacity(0.9)],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(Color.inkMuted.opacity(0.18), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.4), radius: 20, x: 0, y: 16)
    }
}

#Preview {
    TunerView()
}
