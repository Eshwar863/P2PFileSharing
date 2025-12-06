# PeerLink - Intelligent Adaptive File Sharing System

<div align="center">

![PeerLink Logo](https://img.shields.io/badge/PeerLink-Adaptive%20File%20Sharing-667eea?style=for-the-badge&logo=files&logoColor=white)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)](https://reactjs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

**A network-aware file transfer system that dynamically optimizes uploads based on real-time network conditions.**

[Features](#-features) • [Architecture](#-architecture) • [Installation](#-installation) • [Usage](#-usage) • [API Reference](#-api-reference) 

</div>

---

## 📋 Table of Contents

- [Problem Statement](#-problem-statement)
- [Solution Overview](#-solution-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage](#-usage)
- [API Reference](#-api-reference)
- [Testing](#-testing)
- [Future Enhancements](#-future-enhancements)

---

## ⚠️ Problem Statement

Traditional file-sharing systems suffer from critical limitations:

| Problem | Impact |
|---------|--------|
| **Static Chunking** | Fixed chunk sizes cause timeouts on slow networks (3G/4G) |
| **No Adaptation** | Uniform compression wastes bandwidth on pre-compressed files |
| **Manual Sharing** | No device discovery; requires external link copying |
| **Poor Resume Support** | Connection drops restart uploads from 0% |
| **Security Gaps** | Public links lack expiration or revocation controls |

---

## ✅ Solution Overview

**PeerLink** introduces **network-aware intelligence** to file transfers:

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                   │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────────┐    │
│  │ File Select │───▶│ Network      │───▶│ Upload with     │    │
│  │             │    │ Measurement  │    │ Custom Headers  │    │
│  └─────────────┘    └──────────────┘    └─────────────────┘    │
│                            │                      │              │
│                   X-Network-Speed         X-Latency-Ms          │
│                   X-Device-Type                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         SERVER                                   │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────────┐    │
│  │ Intelligence│───▶│ Adaptive     │───▶│ Compress &      │    │
│  │ Engine      │    │ Chunking     │    │ Store           │    │
│  └─────────────┘    └──────────────┘    └─────────────────┘    │
│        │                                         │              │
│   ChunkSize: 16KB-8MB                    GZIP Compression       │
│   Compression: 1-9                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

### Core Features

| Feature | Description |
|---------|-------------|
| **🔄 Adaptive Chunking** | Dynamically adjusts chunk sizes (16KB - 8MB) based on network speed and latency |
| **📦 Adaptive Buffer Sizing** | Server memory allocation aligns with incoming chunk sizes |
| **🔐 JWT Authentication** | Stateless, secure token-based authentication with Spring Security |
| **👥 User Discovery** | IP-based detection of nearby users for direct peer-to-peer sharing |
| **🔗 Public Sharing** | Time-bound, revocable links for guest access |
| **🧹 Auto Cleanup** | Scheduled background jobs remove expired files and invalid tokens |
| **📧 Email Sharing** | Send share links directly via integrated email service |

### Adaptive Algorithm

The intelligence engine calculates optimal parameters based on:

```java
// Network Classification
if (speed > 100 Mbps && latency < 20ms)  → EXCELLENT → 8MB chunks
if (speed > 50 Mbps && latency < 50ms)   → FAST      → 4MB chunks
if (speed > 10 Mbps && latency < 100ms)  → MEDIUM    → 2MB chunks
if (speed > 5 Mbps && latency < 200ms)   → SLOW      → 512KB chunks
else                                      → VERY_SLOW → 64KB chunks
```

---

## 🏗️ Architecture

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER (React)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │  Login   │  │  Upload  │  │ Download │  │ Discovery│     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
└──────────────────────────────────────────────────────────────┘
                              │
                    HTTP/REST (JSON + JWT)
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│               APPLICATION LAYER (Spring Boot)                 │
│  ┌────────────────┐  ┌─────────────────┐  ┌───────────────┐ │
│  │ AuthController │  │ UploadController│  │DownloadCtrl   │ │
│  └────────────────┘  └─────────────────┘  └───────────────┘ │
│  ┌────────────────┐  ┌─────────────────┐  ┌───────────────┐ │
│  │ Spring Security│  │ IntelligenceSvc │  │ FileStorageSvc│ │
│  └────────────────┘  └─────────────────┘  └───────────────┘ │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                     DATA LAYER                                │
│  ┌─────────────────────┐    ┌─────────────────────┐         │
│  │   MySQL Database    │    │   Local File System │         │
│  │   (Metadata)        │    │   (Compressed Files)│         │
│  └─────────────────────┘    └─────────────────────┘         │
└──────────────────────────────────────────────────────────────┘
```

### Data Flow Diagram

```
                    ┌─────────┐
                    │  USER   │
                    └────┬────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │  AUTH    │   │  UPLOAD  │   │ DOWNLOAD │
   └────┬─────┘   └────┬─────┘   └────┬─────┘
        │              │              │
        ▼              ▼              ▼
   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │  JWT     │   │ OPTIMIZE │   │ VERIFY   │
   │ Generate │   │ PARAMS   │   │ ACCESS   │
   └────┬─────┘   └────┬─────┘   └────┬─────┘
        │              │              │
        ▼              ▼              ▼
   ┌─────────────────────────────────────────┐
   │              DATABASE / STORAGE          │
   └─────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| Java 21 | Core language |
| Spring Boot 3.0 | Application framework |
| Spring Security | Authentication & Authorization |
| JWT | Stateless token management |
| Hibernate/JPA | ORM for database operations |
| MySQL 8.0 | Relational database |
| GZIP | File compression |
| JavaMail | Email service |

### Frontend
| Technology | Purpose |
|------------|---------|
| React 18 | UI framework |
| Vite | Build tool |
| Axios | HTTP client |
| React Router | Navigation |
| Lucide React | Icons |
| React Hot Toast | Notifications |

---

## 📦 Installation

### Prerequisites

- Java 21+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### Backend Setup

```bash
# Clone the repository
git clone https://github.com/Eshwar863/PeerLink.git
cd peerlink/backend

# Configure database (see Configuration section)
# Edit src/main/resources/application.properties

# Build and run
mvn clean install
mvn spring-boot:run
```

### Frontend Setup

```bash
cd peerlink/frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

---

## ⚙️ Configuration

### Backend Configuration (`application.properties`)

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/peerlink
spring.datasource.username=root
spring.datasource.password=your_password

# JWT
jwt.secret=your-256-bit-secret-key
jwt.expiration=86400000

# File Storage
upload.directory=./uploads
max.file.size=10737418240

# Email 
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### Frontend Configuration (`src/services/api.js`)

```javascript
const API_BASE_URL = 'http://localhost:8080';
```

---

## 🚀 Usage

### 1. User Registration

```bash
POST /api/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

### 2. Login

```bash
POST /api/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "securePassword123"
}

# Response: JWT Token
```

### 3. Upload File

```bash
POST /files/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data
X-Network-Speed: 50.0
X-Latency-Ms: 30
X-Device-Type: DESKTOP

file: <binary>
```

### 4. Download File

```bash
GET /files/download/{transferId}
Authorization: Bearer <token>
```

### 5. Public Sharing

```bash
# Mark file as public
POST /fileshare/markPublic/{transferId}

# Get share link
POST /fileshare/share/{transferId}/link

# Public download (no auth required)
GET /files/download/{shareToken}/public
```

---

## 📚 API Reference

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/register` | Register new user |
| POST | `/api/login` | Login and get JWT |
| POST | `/api/forgot-password` | Request password reset |
| POST | `/api/reset-password` | Reset password with token |

### File Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/upload` | Upload file |
| GET | `/files/download/{id}` | Download file (authenticated) |
| GET | `/files/download/{token}/public` | Download public file |
| GET | `/files/history` | Get upload history |
| GET | `/files/info/{id}` | Get file metadata |

### Sharing Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/fileshare/markPublic/{id}` | Mark file as public |
| POST | `/fileshare/markPrivate/{id}` | Mark file as private |
| POST | `/fileshare/share/{id}/link` | Generate share link |
| POST | `/fileshare/share/file/email` | Send share via email |

### Discovery Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/discovery/nearby` | Get nearby devices |
| POST | `/request/send` | Send file request to peer |
| GET | `/request/pending` | Get pending requests |

---

## 🧪 Testing

### Adaptive Chunking Test Results

| File Type | Network | Chunk Size | Result |
|-----------|---------|------------|--------|
| PDF | 100 Mbps / 10ms | 2 MB | ✅ Optimal |
| PDF | 2 Mbps / 300ms | 32 KB | ✅ Minimized |
| Video | 50 Mbps / 50ms | 512 KB | ✅ Balanced |
| ISO | 10 Mbps / 100ms | 128 KB | ✅ Adapted |

### Running Tests

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm run test
```

---

## 🔮 Future Enhancements

### 1. Real-Time Adaptation Module
Currently, network metrics are measured once at upload start. The planned enhancement will:
- Split files into multiple chunks on client-side
- Measure throughput after each chunk
- Dynamically recalculate next chunk size
- Handle network drops mid-transfer without timeout

### 2. Redis Pause & Resume
- Persist upload state (`uploadId`, `bytesUploaded`) in Redis
- Enable resumption from last successful chunk
- Zero wasted bandwidth on disconnects

### 3. WebRTC P2P Transfer
- Direct peer-to-peer file transfer for local users
- Bypass server for same-network transfers

### 4. End-to-End Encryption
- Client-side encryption before upload
- Only recipient can decrypt

---

## 🙏 Acknowledgments

- [HTTP Multipart Form Data Explained](https://lnkd.in/gDsttvi7)
- [Spring Boot File Upload Tutorial](https://lnkd.in/gnA8bkZc)
- Spring Boot & React communities

---

