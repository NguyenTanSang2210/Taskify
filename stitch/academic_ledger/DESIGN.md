# Design System Document: The Academic Architect

## 1. Overview & Creative North Star
**Creative North Star: The Academic Architect**
This design system moves beyond the cluttered, "spreadsheet-heavy" feel of traditional project management. It is built on the principle of **Intelligent Clarity**. By leveraging high-contrast typography scales and sophisticated tonal layering, we create an environment that feels authoritative yet breathable. 

The system rejects the "boxed-in" aesthetic of legacy software. Instead, it utilizes intentional asymmetry and "The No-Line Rule" to guide the student’s eye through complex workflows without the visual noise of traditional grids. It is a digital workspace that feels as focused and intentional as a premium library or a modern research lab.

---

## 2. Colors: Tonal Depth & Narrative
Our palette is anchored in **Tech Blue** and a spectrum of **Architectural Grays**. The goal is to use color not just for decoration, but as a functional tool for hierarchy.

### The Color Palette (Material Design Tokens)
*   **Primary (Brand Anchor):** `#455f88` (Primary) — Used for authoritative actions and key brand moments.
*   **Surface Foundation:** `#f7fafc` (Surface) — The base canvas for all views.
*   **Accents:** `#5d5d78` (Tertiary) — Reserved for secondary academic metadata or "mentions."

### The "No-Line" Rule
To achieve a premium, editorial feel, **1px solid borders are strictly prohibited** for sectioning. Boundaries between the navigation, the workspace, and the inspector panel must be defined solely through background color shifts.
*   *Implementation:* Place a `surface-container-low` (#eff4f7) sidebar against a `surface` (#f7fafc) main workspace. The transition in tone is enough for the eye to perceive a boundary without the "trapped" feeling of a line.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. We use the surface-container tiers to create "nested" depth:
1.  **Level 0 (Base):** `surface` (#f7fafc)
2.  **Level 1 (Sections/Panels):** `surface-container` (#e7eff3)
3.  **Level 2 (Active Cards):** `surface-container-lowest` (#ffffff)
This "nesting" creates a natural lift. A white card (`surface-container-lowest`) sitting on a light blue-gray section (`surface-container`) creates instant focus without requiring a single stroke.

### The "Glass & Gradient" Rule
For floating elements like modals or dropdowns, use **Glassmorphism**.
*   **Token usage:** `surface` at 80% opacity with a 16px backdrop-blur.
*   **Signature Textures:** For high-level CTA buttons (e.g., "Start New Project"), apply a subtle linear gradient from `primary` (#455f88) to `primary_dim` (#39537c). This adds a "soul" and tactile quality that differentiates the tool from generic templates.

---

## 3. Typography: The Editorial Scale
We use **Inter** as our sole typeface, relying on extreme scale and weight contrast to establish an academic hierarchy.

*   **Display (Display-LG: 3.5rem):** Used for empty-state inspiration or major dashboard milestones.
*   **Headlines (Headline-SM: 1.5rem):** Used for Project Titles. Tighten letter-spacing by -2% for an authoritative, "printed" feel.
*   **Titles (Title-SM: 1rem):** Used for card headers. Always use `on_surface` (#283439) for maximum legibility.
*   **Body (Body-MD: 0.875rem):** The workhorse for task descriptions. Use a generous line height (1.5) to maintain readability in data-dense environments.
*   **Labels (Label-SM: 0.6875rem):** All-caps with +5% letter-spacing. Used for "Status Tags" or "Due Dates." This conveys a sense of technical precision.

---

## 4. Elevation & Depth
Depth is achieved through **Tonal Layering**, not structural lines.

*   **The Layering Principle:** Avoid the "flat" look by stacking. A `surface-container-highest` panel should be used only for the most transient elements (like a side-drawer), while the `surface-container-lowest` (pure white) is reserved for the active task the student is currently editing.
*   **Ambient Shadows:** When an element must "float" (e.g., a dragged task card), use an extra-diffused shadow: `box-shadow: 0 12px 32px rgba(40, 52, 57, 0.06)`. Notice the shadow uses a tinted version of `on-surface` (#283439) rather than pure black, ensuring the shadow feels like natural ambient light.
*   **The "Ghost Border" Fallback:** If a container requires further definition (e.g., in high-density tables), use the `outline_variant` (#a7b4ba) at **15% opacity**. It should be felt, not seen.

---

## 5. Components

### Buttons
*   **Radius:** Always `lg` (0.5rem / 8px).
*   **Primary:** `primary` (#455f88) fill with `on_primary` (#f6f7ff) text. No border.
*   **Secondary:** `surface-container-high` (#dfeaef) fill. No border. This creates a "soft" button that blends with the UI until hovered.

### Cards & Task Items
*   **Constraint:** Zero borders. Zero dividers.
*   **Separation:** Use a 16px or 24px vertical gap from the spacing scale.
*   **Interaction:** On hover, transition the background from `surface-container-lowest` (#ffffff) to `primary_container` (#d6e3ff) at 30% opacity for a sophisticated "highlight" effect.

### Input Fields
*   **Style:** "Understated Academic." No full-box border. Use a `surface-variant` (#d7e5eb) background with a 2px bottom-accent in `outline` (#707d82). 
*   **Focus State:** The bottom accent transitions to `primary` (#455f88) with a soft `surface-tint` glow.

### Academic Progress Chips
*   **Visuals:** Use `tertiary_container` (#d9d7f8) for "Research" tags and `primary_container` (#d6e3ff) for "Administrative" tags. Use `label-sm` for the text to maintain a technical, metadata-heavy feel.

---

## 6. Do's and Don'ts

### Do:
*   **Embrace White Space:** Use the absence of content to define the importance of content.
*   **Use Tonal Nesting:** Place lighter surfaces on darker backgrounds to draw the eye inward toward the work.
*   **Align to the Grid, but Break for Impact:** Keep task lists strictly aligned, but allow "Project Summaries" or "Hero Stats" to use asymmetrical padding to feel bespoke.

### Don't:
*   **Don't use 1px Dividers:** Never use a line to separate two list items. Use 8px of `surface-container` background space instead.
*   **Don't use Pure Black:** Always use `on_surface` (#283439) for text. Pure black is too harsh for an academic environment and causes eye strain.
*   **Don't use High-Contrast Shadows:** Avoid the "floating on a cloud" look. Shadows should be so subtle they are almost imperceptible.
*   **Don't Default to 4px Corners:** Stick strictly to the **8px (ROUND_EIGHT)** rule for all interactive components to maintain a friendly yet professional "Notion-esque" softness.