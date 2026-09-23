# Forms & Inputs

Form elements provide interactive fields for data entry, including text inputs, labels, and helper text.

---

## Input Fields

An input field in Vireo is defined with sizing, borders, padding, and an optional `placeholder`:

```dac
component EmailInput {
    width: fill
    height: 44
    padding: 0 14
    color: #FFFFFF
    border: 1 #D1D5DB
    radius: 8
    placeholder: "you@example.com"
}
```

When rendered to HTML, an input with `placeholder:` displays a placeholder text styled in neutral gray (`#9CA3AF`) and vertically centered.

---

## Form Group Pattern

In real UI design, inputs are paired with a label and optional error or helper messages:

```dac
component FormGroup {
    width: fill
    layout: vertical
    gap: 6

    component FieldLabel {
        text: "Email Address"
        fontSize: 14
        fontWeight: medium
        color: #374151
    }

    component InputField {
        width: fill
        height: 40
        padding: 0 12
        color: #FFFFFF
        border: 1 #D1D5DB
        radius: 6
        placeholder: "alex@company.com"
    }

    component HelperText {
        text: "We will never share your email address."
        fontSize: 12
        color: #6B7280
    }
}
```

---

## Form Layouts

Combine inputs and buttons into complete forms:

```dac
import buttons from "./components/buttons.dac"

block Auth {
    component LoginForm {
        width: 360
        padding: 32
        radius: 12
        color: #FFFFFF
        border: 1 #E5E7EB
        shadow: true
        layout: vertical
        gap: 20

        component Title {
            text: "Sign In"
            fontSize: 24
            fontWeight: bold
            color: #111827
        }

        component EmailField {
            ref: FormGroup
        }

        component Submit {
            ref: buttons.Primary.Default
            width: fill

            component Label {
                text: "Continue with Email"
            }
        }
    }
}
```
