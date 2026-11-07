# Java Spring Boot

Repository ini berisi kumpulan source code pendukung untuk artikel-artikel bertag **Java Spring Boot** di website:

👉 **https://sch39.dev/articles?tag=java-spring-boot**

Setiap project adalah aplikasi Spring Boot yang berdiri sendiri dan menjadi contoh praktik dari satu atau beberapa artikel. Silakan baca artikelnya terlebih dahulu untuk memahami konteks, penjelasan, dan alasan di balik kode yang ditulis.

## Struktur Repository

```text
java-spring-boot/
├── tus-manual/                                   # Resumable upload (tus) manual
├── tus_library/                                  # Resumable upload (tus) dengan library
├── Reactive Backend Engineering with Java & Spring/
│   ├── RM01-01-reactor-basics/
│   ├── movies-info-service/
│   ├── movies-review-service/
│   ├── compose-reactive.yml                      # MongoDB + PostgreSQL
│   └── .spring-boot-reactive-env.example.example
└── kafka-engineering-notes/
    ├── order-service/                            # Kafka producer
    ├── notification-service/                     # Kafka consumer
    └── reset-kafka.sh                            # Reset topic & consumer group
```

## Daftar Project

### Resumable Upload (tus)

| Folder | Build Tool | Deskripsi |
| --- | --- | --- |
| [`tus-manual`](./tus-manual) | Maven | Implementasi protokol resumable upload [tus](https://tus.io) secara manual (endpoint `POST`, `HEAD`, `PATCH` di `/files`) menggunakan Spring MVC dan Thymeleaf, dengan client `tus-js-client`. |
| [`tus_library`](./tus_library) | Maven | Resumable upload tus menggunakan library [`tus-java-server`](https://github.com/tomdesair/tus-java-server), lengkap dengan konfigurasi dan scheduled cleanup untuk upload yang kedaluwarsa. |

### Reactive Backend Engineering with Java & Spring

Folder: [`Reactive Backend Engineering with Java & Spring`](<./Reactive Backend Engineering with Java & Spring>)

| Folder | Build Tool | Deskripsi |
| --- | --- | --- |
| [`RM01-01-reactor-basics`](<./Reactive Backend Engineering with Java & Spring/RM01-01-reactor-basics>) | Gradle | Dasar-dasar Reactor / reactive programming di Spring WebFlux. |
| [`movies-info-service`](<./Reactive Backend Engineering with Java & Spring/movies-info-service>) | Gradle | Service reaktif berbasis Spring WebFlux dan MongoDB Reactive dengan operasi CRUD (`/v1/movie-info`) dan `GET /v1/movie-info/{id}/with-reviews` yang memanggil review service lewat gRPC client. Port `8082`. |
| [`movies-review-service`](<./Reactive Backend Engineering with Java & Spring/movies-review-service>) | Gradle | Service reaktif berbasis Spring WebFlux dan Postgres (R2DBC) untuk review film, dengan REST (`/v1/reviews`, port `8081`) dan gRPC server (`ReviewService`: `AddReview`, `ListReviews`, `StreamReviews`). |

### Kafka Engineering Notes

Folder: [`kafka-engineering-notes`](./kafka-engineering-notes)

| Folder | Build Tool | Deskripsi |
| --- | --- | --- |
| [`order-service`](./kafka-engineering-notes/order-service) | Gradle | Kafka **producer** (Spring Kafka). Membuat topic `orders` (3 partisi, 1 replika) dan mempublikasikan event `OrderCreated` dengan key `customerId` dan `acks=all`. Endpoint: `POST /orders` dan `POST /orders/bulk?customerId=&count=`. Port default `8080`. |
| [`notification-service`](./kafka-engineering-notes/notification-service) | Gradle | Kafka **consumer** untuk topic `orders` dengan manual ack (`ack-mode: manual`, `enable-auto-commit: false`). Endpoint: `GET /notifications`, `GET /consumer/assignment` (assignment partisi + lag), `POST /consumer/pause`, `POST /consumer/resume`. Port `8082`. |
| [`reset-kafka.sh`](./kafka-engineering-notes/reset-kafka.sh) | Bash | Skrip untuk menghapus semua topic dan consumer group di broker (kecuali topic internal `__*`). |

## Teknologi

- Java 21
- Spring Boot 4.1.x
- Spring MVC + Thymeleaf (`tus-manual`, `tus_library`)
- Spring WebFlux + Reactor + MongoDB Reactive (`movies-info-service`, `RM01-01-reactor-basics`)
- Spring WebFlux + R2DBC + PostgreSQL + gRPC / Protobuf (`movies-review-service`)
- Spring Kafka + Apache Kafka (`order-service`, `notification-service`)
- [tus-js-client](https://github.com/tus/tus-js-client) di sisi browser

## Prasyarat

- JDK 21
- Maven (atau gunakan wrapper `mvnw` yang sudah disertakan) untuk project Maven
- Gradle (atau gunakan wrapper `gradlew` yang sudah disertakan) untuk project Gradle
- Docker (opsional) untuk menjalankan MongoDB dan PostgreSQL lewat `compose-reactive.yml`
- MongoDB untuk menjalankan `movies-info-service`
- PostgreSQL untuk menjalankan `movies-review-service`
- Apache Kafka (broker di `localhost:9092`) untuk menjalankan `order-service` dan `notification-service`

## Cara Menjalankan

Masuk ke folder project yang diinginkan, lalu jalankan:

```bash
# Project Maven (tus-manual, tus_library)
./mvnw spring-boot:run

# Project Gradle (movies-*, RM01-01-reactor-basics, order-service, notification-service)
./gradlew bootRun
```

Di Windows gunakan `mvnw.cmd` atau `gradlew.bat`.

Untuk project upload (`tus-manual` dan `tus_library`), buka halaman `http://localhost:8080/upload` setelah aplikasi berjalan.

### Database (MongoDB & PostgreSQL)

File Docker Compose dan contoh env berada di dalam folder `Reactive Backend Engineering with Java & Spring`:

```bash
cd "Reactive Backend Engineering with Java & Spring"

# 1. Salin contoh env, lalu sesuaikan nilainya (file ini di-ignore git)
cp .spring-boot-reactive-env.example.example .spring-boot-reactive-env

# 2. Jalankan MongoDB (27017) dan PostgreSQL (5433)
docker compose --env-file .spring-boot-reactive-env -f compose-reactive.yml up -d
```

`movies-review-service` membaca `POSTGRES_DB`, `POSTGRES_USER`, dan `POSTGRES_PASSWORD` dari environment, jadi export dulu sebelum `bootRun`:

```bash
cd movies-review-service
set -a; source ../.spring-boot-reactive-env; set +a
./gradlew bootRun
```

Port default: `movies-review-service` REST di `8081` (gRPC memakai port default Spring gRPC), `movies-info-service` di `8082`.

### Kafka (order-service & notification-service)

Pastikan broker Kafka berjalan di `localhost:9092`, lalu jalankan consumer dan producer di terminal terpisah:

```bash
# Terminal 1: consumer (port 8082)
cd kafka-engineering-notes/notification-service
./gradlew bootRun

# Terminal 2: producer (port 8080)
cd kafka-engineering-notes/order-service
./gradlew bootRun
```

Kirim order dan lihat hasilnya:

```bash
# Satu order
curl -X POST localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"c-1","amount":150}'

# Banyak order sekaligus
curl -X POST 'localhost:8080/orders/bulk?customerId=c-1&count=10'

# Notifikasi yang sudah diterima consumer
curl localhost:8082/notifications

# Assignment partisi dan lag consumer
curl localhost:8082/consumer/assignment

# Pause / resume consumer
curl -X POST localhost:8082/consumer/pause
curl -X POST localhost:8082/consumer/resume
```

Beberapa properti `notification-service` yang bisa diubah di `application.yml` (atau lewat argumen/environment) untuk eksperimen:

- `server.port`: ubah untuk menjalankan beberapa instance consumer dalam satu group dan mengamati rebalancing partisi.
- `app.ack-enabled`: set `false` untuk mensimulasikan consumer yang tidak melakukan ack (offset tidak di-commit).

> **Catatan port:** `notification-service` dan `movies-info-service` sama-sama memakai port `8082`, dan `order-service` memakai `8080` (sama dengan `tus-*`). Jangan jalankan project yang bentrok bersamaan, atau ubah `server.port` salah satunya.

Untuk mengulang percobaan dari awal, jalankan `kafka-engineering-notes/reset-kafka.sh`. Skrip ini menghapus **semua** topic dan consumer group di broker, jadi jangan dipakai pada broker yang berisi data penting. Ubah baris `cd ~/kafka_2.13-4.3.1` di dalam skrip sesuai lokasi instalasi Kafka Anda; broker dapat diubah lewat env `BOOTSTRAP_SERVER`.

## Catatan

Kode di repository ini dibuat untuk keperluan belajar dan demonstrasi pada artikel, bukan untuk langsung dipakai di production tanpa penyesuaian (keamanan, validasi, konfigurasi, dsb.).

## Artikel

Daftar lengkap artikel: **https://sch39.dev/articles?tag=java-spring-boot**
