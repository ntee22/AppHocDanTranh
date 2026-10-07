# Dan Thanh Guzheng - Design System

## Visual Identity

### Philosophy
The design embodies **Zen minimalism** with a touch of traditional Chinese aesthetics, creating a peaceful and focused learning environment.

---

## Color Palette

### Primary Colors

```
Soft Teal (#78A083)
█████ Primary brand color
Use for: Main CTAs, active states, primary text emphasis
RGB: 120, 160, 131

Warm Wood Brown (#8B4513)
█████ Secondary brand color  
Use for: Titles, important text, decorative elements
RGB: 139, 69, 19

Off-white/Cream (#FDFCF0)
█████ Background color
Use for: Main backgrounds, creating breathing room
RGB: 253, 252, 240
```

### Supporting Colors

```
Accent Teal (#5A7D64)
█████ Darker teal for emphasis
Use for: Hover states, dark mode accents

Light Teal (#A8C5AE)
█████ Subtle backgrounds
Use for: Cards, sections, dividers

Card Background (#FFFEF8)
█████ Very light cream
Use for: Elevated cards, content containers

Locked Gray (#9CA3AF)
█████ Disabled/locked content
Use for: Locked lessons, disabled states

Success Green (#6EAF7D)
█████ Free content indicator
Use for: Free badges, success states

Premium Gold (#D4AF37)
█████ Premium features
Use for: Premium badges, purchase CTAs
```

---

## Typography

### System Fonts

**Titles (Serif)** - Traditional, elegant feel
- Hero Title: System Serif, Bold, 34pt
- Section Title: System Serif, Semibold, 28pt
- Card Title: System Serif, Semibold, 20pt
- Subtitle: System Serif, Medium, 17pt

**Body (Sans-serif)** - Clean, readable
- Body Text: System Default, Regular, 17pt
- Caption: System Default, Regular, 12pt
- Metadata: System Default, Regular, 11pt

**Special**
- Lesson Numbers: System Rounded, Bold, 22pt
- Button Text: System Default, Semibold, 17pt

### Text Colors
- Primary: System Default (adapts to dark/light mode)
- Secondary: Gray (0.6 opacity)
- Emphasis: Primary Teal or Secondary Brown

---

## Components

### 1. Lesson Card

**Dimensions**: Full width with 16pt horizontal padding
**Height**: Dynamic (min 100pt)
**Corner Radius**: 20pt
**Shadow**: Color: Teal (0.1 opacity), Radius: 8pt, Offset: (0, 4)

**Structure**:
```
┌─────────────────────────────────────────┐
│  ┌────┐                                 │
│  │ 01 │  Introduction to Guzheng        │
│  │    │  History and parts of the...    │
│  └────┘  ⏱ 8:45  🎁 Free               →│
└─────────────────────────────────────────┘
```

**States**:
- Default: White background, teal border (0.2 opacity)
- Locked: Gray text, lock icon
- Premium: Gold accent, "Unlocked" badge

### 2. Video Player

**Aspect Ratio**: 16:9
**Height**: 250pt
**Background**: Black
**Controls**: Custom overlay with teal accents

**Watermark**:
- Font: System Monospaced, 12pt
- Color: White (0.4 opacity)
- Background: Black (0.3 opacity), 6pt corner radius
- Position: Animated (moves every 8 seconds)

### 3. Purchase Bottom Sheet

**Height**: 500pt
**Background**: Cream (#FDFCF0)
**Corner Radius**: 20pt (top corners)
**Drag Indicator**: Gray bar, 40×5pt

**Structure**:
```
     ───────
     
      ✨
  Premium Icon
  
Unlock Full Course
Get lifetime access to all 12 lessons

✓ 12 Comprehensive Lessons
✓ High-Quality Videos  
✓ Lifetime Access
✓ Expert Instruction

┌─────────────────────────────┐
│   Buy 3-Month Course        │
└─────────────────────────────┘
      Restore Purchase
```

### 4. Status Badges

**Free Badge**:
- Background: Success Green (0.15 opacity)
- Text: Success Green, 11pt, Semibold
- Padding: 8pt horizontal, 4pt vertical
- Shape: Capsule
- Icon: 🎁 gift.fill

**Premium Badge**:
- Background: Premium Gold (0.15 opacity)
- Text: Premium Gold, 11pt, Semibold
- Padding: 8pt horizontal, 4pt vertical
- Shape: Capsule
- Icon: ✓ checkmark.seal.fill

**Lock Badge**:
- Size: 44×44pt circle
- Background: Gradient (Gray 0.3 to 0.1)
- Icon: lock.fill, 18pt
- Animation: Scale pulse (1.0 to 1.1, 1.5s repeat)

---

## Spacing System

```
4pt  - Tiny gaps (icon-text spacing)
8pt  - Small gaps (between related elements)
12pt - Medium gaps (within cards)
16pt - Standard gaps (card padding, between sections)
20pt - Large gaps (between major sections)
24pt - Extra large (section padding)
32pt - Hero spacing (around hero elements)
```

---

## Layout Guidelines

### Screen Margins
- Horizontal: 16pt (standard)
- Vertical: 20pt (scrolling views)
- Safe Area: Always respect safe area insets

### Card Spacing
- Between cards: 16pt
- Card horizontal margin: 16pt from screen edge
- Card internal padding: 16pt all sides

### Button Heights
- Primary buttons: 52pt (comfortable tap target)
- Secondary buttons: 44pt
- Minimum tap target: 44×44pt

---

## Animations

### Timing
- Quick transitions: 0.2s (UI feedback)
- Standard animations: 0.3s (most transitions)
- Slow animations: 0.5s (meaningful changes)
- Watermark movement: 3.0s ease-in-out

### Curves
- Spring: For interactive elements (buttons, cards)
- Ease-in-out: For smooth transitions
- Linear: For continuous animations (loading)

### Key Animations
1. **Lesson Card Tap**: Scale 0.97 on press
2. **Lock Badge**: Continuous pulse (1.0 to 1.1)
3. **Watermark**: Move between corners every 8s
4. **Bottom Sheet**: Slide up with spring damping
5. **Loading**: Circular progress with teal color

---

## Icons

Using **SF Symbols** throughout:

```
play.rectangle.fill     - Lessons tab
tuningfork             - Tuner tab
music.note.list        - Sheet Music tab
music.quarternote.3    - Hero decoration
lock.fill              - Locked content
gift.fill              - Free content
checkmark.seal.fill    - Unlocked content
sparkles               - Premium features
clock                  - Duration
chevron.right          - Navigation
play.fill / pause.fill - Video controls
arrow.up.left.and.arrow.down.right - Fullscreen
exclamationmark.triangle - Errors
```

---

## Accessibility

### Font Scaling
- Support Dynamic Type
- Test at all accessibility sizes
- Maintain layout at large text sizes

### Colors
- All text meets WCAG AA contrast ratios
- Don't rely solely on color for information
- Provide alternative indicators (icons, text)

### VoiceOver
- Meaningful labels for all interactive elements
- Descriptive hints for complex interactions
- Logical navigation order

### Interaction
- Minimum tap target: 44×44pt
- Clear focus indicators
- Support for Voice Control

---

## Platform Guidelines

Following Apple Human Interface Guidelines:

✅ **Do:**
- Use native iOS patterns (TabView, NavigationStack, Sheet)
- Respect system settings (appearance, text size)
- Provide clear feedback for all actions
- Use standard gestures (tap, swipe, long press)
- Design for one-handed use when possible

❌ **Don't:**
- Use non-standard UI patterns
- Ignore safe areas
- Block user input without clear indication
- Use too many colors or fonts
- Create tiny tap targets

---

## Dark Mode (Future)

When implementing dark mode:

```
Background: #1C1C1E (System dark background)
Card Background: #2C2C2E
Primary Teal: Lighten to #8BB5A0 (better contrast)
Secondary Brown: Lighten to #B8835D
Text: White with varying opacity
```

---

## Implementation Notes

### SwiftUI Modifiers

Create reusable view modifiers for consistency:

```swift
.cardStyle()           // Applies card background, shadow, border
.primaryButton()       // Primary button styling
.secondaryButton()     // Secondary button styling
.sectionSpacing()      // Consistent section spacing
.heroTitleStyle()      // Hero title formatting
```

### Testing Checklist

- [ ] Test on iPhone SE (smallest screen)
- [ ] Test on iPhone 15 Pro Max (largest screen)
- [ ] Test with Dynamic Type at largest size
- [ ] Test all animations for smoothness
- [ ] Verify color contrast ratios
- [ ] Test VoiceOver navigation
- [ ] Verify safe area handling
- [ ] Check landscape orientation (if supported)

---

This design system ensures a cohesive, beautiful, and accessible experience across the entire app.
