<p align="center">
  <h1 align="center">ShopLite</h1>
  <p align="center">
    A modern, lightweight, and comprehensive Point of Sale (POS) and Store Management system.
    <br />
    <br />
    <a href="https://shoplite-36f6c.web.app/"><strong>View Live Demo »</strong></a>
    <br />
  </p>
</p>

## Live Deployments

*   **Frontend (Firebase Hosting):** [https://shoplite-36f6c.web.app/](https://shoplite-36f6c.web.app/)
*   **Backend API (Google Cloud Run):** [https://shoplite-api-1082377852931.asia-southeast1.run.app/](https://shoplite-api-1082377852931.asia-southeast1.run.app/)

> **Note:** The backend is hosted on a serverless platform (Cloud Run). The first API request after a period of inactivity may take 10-15 seconds to process due to a "cold start". Subsequent requests will be incredibly fast.

---

## Overview & Key Features

ShopLite empowers retail businesses to efficiently manage day-to-day operations with an easy-to-use, mobile-first interface.

*   **Point of Sale (POS):** Fast and intuitive checkout process. Supports multiple payment methods (Cash, Bank Transfer/QR, E-wallet).
*   **Order Management:** Track order history, manage pending payments, and handle order returns and refunds seamlessly.
*   **Inventory Control:** Real-time stock tracking. Features include low stock alerts, inventory adjustments (Kiểm kho), and comprehensive import order management (Nhập hàng).
*   **Employee Management:** Strict Role-based Access Control (RBAC), robust attendance tracking via GPS Check-in/Check-out, and payroll/salary history logs.
*   **Comprehensive Reporting:** Detailed analytics including End of Day reports, Sales performance, and Inventory movements (Biến động hàng hóa) visualized with clean, interactive UI elements.
*   **Responsive UI/UX:** A mobile-first design built with Ionic React, ensuring a smooth experience across devices (Mobile, Tablet, Desktop).

## Technology Stack

### Backend
*   **Core Framework:** Java 17, Spring Boot 3.x
*   **Build Tool:** Gradle
*   **Security:** Spring Security with stateless JWT Authentication
*   **Database Integration:** JPA/Hibernate (configured for relational databases like MySQL)
*   **Architecture Pattern:** RESTful API with distinct layers (Controller, Service, Repository, Domain)
*   **Deployment:** Google Cloud Run (Dockerized)

### Frontend
*   **Core Framework:** React 18
*   **Mobile UI Framework:** Ionic Framework
*   **Routing:** React Router DOM
*   **Styling:** Modern Vanilla CSS with custom design tokens, responsive layouts, and clean UI components
*   **Deployment:** Firebase Hosting

## Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites
*   [Java JDK 17+](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
*   [Node.js (v16+)](https://nodejs.org/) & npm
*   [Ionic CLI](https://ionicframework.com/docs/cli) (`npm install -g @ionic/cli`)
*   MySQL Database (Optional: H2 for in-memory testing)

### Running the Backend
1. Navigate to the backend directory:
   ```bash
   cd backend/shoplite
   ```
2. Configure your database settings in `src/main/resources/application.properties` (or `.yml`). 
3. Run the Spring Boot application using Gradle:
   ```bash
   ./gradlew bootRun
   ```
   *(The backend server will typically start on `http://localhost:8080`)*

### Running the Frontend
1. Navigate to the frontend directory:
   ```bash
   cd frontend/shopliteapp
   ```
2. Install the required NPM packages:
   ```bash
   npm install
   ```
3. Start the Ionic development server:
   ```bash
   ionic serve
   ```
   *(The frontend application will launch on `http://localhost:8100`)*

## Project Structure

```
ShopLite/
├── backend/
│   └── shoplite/                # Spring Boot Backend Project
│       ├── src/main/java/...    # Controllers, Services, Repositories, Domain Entities
│       ├── src/main/resources/  # application.properties, static assets
│       └── build.gradle         # Gradle configuration & dependencies
└── frontend/
    └── shopliteapp/             # Ionic React Frontend Project
        ├── src/
        │   ├── components/      # Reusable UI components (Modals, Headers, Forms)
        │   ├── pages/           # Application pages (Sales, Reports, Inventory)
        │   ├── services/        # Logic & state management
        │   └── utils/           # Helper functions, Constants, and API bindings
        ├── .env.local           # Local environment variables
        ├── .env.production      # Production environment variables
        └── package.json         # Node dependencies
```

## Security & Architecture Highlights
- **Authentication:** Token-based authentication using JWT ensures secure access to APIs without relying on session cookies.
- **Data Integrity:** Transactional Spring services guarantee atomic database operations, which is especially crucial for Inventory Movements and Payment processing workflows.
- **Time Zone Sync:** The application backend is hard-configured to process and save all timestamps in `Asia/Ho_Chi_Minh` (UTC+7), ensuring data consistency regardless of server host location.

## Contributing
Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License
This project is proprietary and confidential. Unauthorized copying, modification, or distribution of this project via any medium is strictly prohibited.