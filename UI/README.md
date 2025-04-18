# Service Virtualization UI

Frontend for the Service Virtualization Platform, providing a user interface to manage virtual services across multiple protocols.

## Features

- Dashboard for managing all protocols (REST, SOAP, TIBCO, IBM MQ, Kafka)
- Protocol-specific configuration interfaces
- Recording and playback controls
- Stub management

## Technology Stack

- React
- TypeScript
- Redux Toolkit with RTK Query
- Tailwind CSS
- Webpack

## Getting Started

### Prerequisites

- Node.js (v16+)
- npm (v8+)

### Installation

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start development server:
   ```bash
   npm run dev
   ```

3. Build for production:
   ```bash
   npm run build
   ```

## Project Structure

- `/src/components`: Reusable UI components
- `/src/pages`: Page-level components
- `/src/api`: API integration with RTK Query
- `/src/config`: Application configuration 