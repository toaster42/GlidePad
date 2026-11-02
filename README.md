# GlidePad 🎛️✨

A clean, responsive trackpad and gesture input solution designed for smooth, intuitive navigation and touch interactions across desktop and web environments.

---

## Features

- **Smooth Input Processing:** Calibrated touch tracking and acceleration curves for natural, fluid gesture response.
- **Customizable Gestures:** Configurable multi-touch actions, scrolling sensitivity, and tap/click behaviors.
- **Minimal Footprint:** Lightweight architecture with low memory and CPU overhead.
- **Cross-Platform Readiness:** Designed for flexible integration across web interfaces and desktop application frameworks.

---

## Getting Started

### Prerequisites

Ensure you have Node.js (v18+ recommended) and `npm` or `yarn` installed on your machine.

```bash
node -v
npm -v
```
Installation
 * Clone the repository:
   git clone [https://github.com/toaster42/GlidePad.git](https://github.com/toaster42/GlidePad.git)
cd GlidePad

 * Install dependencies:
   `npm install`

 * Start the development server:
   `npm run dev`

Usage
Import and initialize GlidePad within your project entry point:
```js
import GlidePad from './src/GlidePad';

const pad = new GlidePad({
  target: '#app',
  sensitivity: 1.2,
  enableGestures: true,
  onGesture: (event) => {
    console.log('Gesture detected:', event.type, event.details);
  }
});

pad.init();
```

Configuration Options
| Option | Type | Default | Description |
|---|---|---|---|
| target | string | HTMLElement | '#glidepad' | DOM container element or selector string. |
| sensitivity | number | 1.0 | Multiplier for scroll and gesture tracking velocity. |
| enableGestures | boolean | true | Toggles multi-finger swipe and pinch processing. |
| invertScroll | boolean | false | Flips natural scrolling direction. |
Development & Scripts
 * `npm run dev` – Starts the local development server with hot reloading.
 * `npm run build` – Compiles production-ready bundle assets to the dist/ folder.
 * `npm run test` – Runs the automated unit test suite.
 * `npm run lint` – Executes ESLint to check for code style and syntax errors.
Contributing
Contributions are welcome! Please follow these steps to contribute:
 * Fork the repository.
 * Create your feature branch (`git checkout -b feature/amazing-feature`).
 * Commit your changes (`git commit -m 'Add amazing feature'`).
 * Push to the branch (`git push origin feature/amazing-feature`).
 * Open a Pull Request.
License
Distributed under the MIT License. See LICENSE for more information.

