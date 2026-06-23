

# Fashion Forge - Android Mobile App

This contains everything you need to run your app locally.


## Features

- **Interactive Design Wizard**: Custom 4-step wizard for bespoke clothing options.
- **AI Design Studio**: Instantly generate boutique-quality designs with AI assistance.
- **Virtual Try-On**: See clothing designs overlaid in real-time.
- **Designer Marketplace**: Explore ready-made, exclusive premium designs.
- **Egyptian Crafts Map**: Find nearby tailors and raw fabric workshops.
- **Supplier & Delivery Portals**: Full logistics system for tailors, suppliers, and delivery partners.

---

## 📱 Application Screenshots

<p align="center">
  Explore the features and interface of <b>Fashion Forge Mobile</b> through the screenshots below:
</p>

<table align="center">
  <tr>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20121845.png" alt="Customer Dashboard" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Customer Dashboard</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20121917.png" alt="Design Wizard" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Design Wizard</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20121946.png" alt="AI Design Studio" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>AI Design Studio</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122001.png" alt="Virtual Try-On" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Virtual Try-On</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122017.png" alt="Designer Marketplace" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Designer Marketplace</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122033.png" alt="Interactive Workshop Map" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Interactive Workshop Map</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122046.png" alt="Sizing & Profile" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Sizing & Profile</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122107.png" alt="Supplier Inventory" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Supplier Inventory</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122125.png" alt="Orders Portal" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Orders Portal</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122145.png" alt="Chat Portal" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Chat Portal</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122207.png" alt="Tailor Dashboard" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Tailor Dashboard</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="assets/screenshots/Screenshot%202026-06-23%20122222.png" alt="Delivery & Supplier Portal" width="240" style="border-radius: 8px;" />
      <br />
      <sub><b>Delivery & Supplier Portal</b></sub>
    </td>
  </tr>
</table>

---

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
