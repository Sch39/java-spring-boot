# Java Spring Boot

Repository ini berisi kumpulan source code pendukung untuk artikel-artikel bertag **Java Spring Boot** di website:

👉 **https://sch39.dev/articles?tag=java-spring-boot**

Setiap folder adalah project Spring Boot yang berdiri sendiri dan menjadi contoh praktik dari satu atau beberapa artikel. Silakan baca artikelnya terlebih dahulu untuk memahami konteks, penjelasan, dan alasan di balik kode yang ditulis.

## Daftar Project

| Folder | Build Tool | Deskripsi |
| --- | --- | --- |
| [`tus-manual`](./tus-manual) | Maven | Implementasi protokol resumable upload [tus](https://tus.io) secara manual (endpoint `POST`, `HEAD`, `PATCH` di `/files`) menggunakan Spring MVC dan Thymeleaf, dengan client `tus-js-client`. |
| [`tus_library`](./tus_library) | Maven | Resumable upload tus menggunakan library [`tus-java-server`](https://github.com/tomdesair/tus-java-server), lengkap dengan konfigurasi dan scheduled cleanup untuk upload yang kedaluwarsa. |
| [`movies-info-service`](./movies-info-service) | Gradle | Service reaktif berbasis Spring WebFlux dan MongoDB Reactive dengan operasi CRUD (`/v1/movie-info`). |
| [`movies-review-service`](./movies-review-service) | Gradle | Service reaktif berbasis Spring WebFlux dan Postgres (R2DBC) untuk review film, dengan REST (`/v1/reviews`, port 8081) dan gRPC server (`ReviewService`: `AddReview`, `ListReviews`, `StreamReviews`). |
| [`RM01-01-reactor-basics`](./RM01-01-reactor-basics) | Gradle | Dasar-dasar Reactor / reactive programming di Spring WebFlux. |

## Teknologi

- Java 21
- Spring Boot 4.1.x
- Spring MVC + Thymeleaf (`tus-manual`, `tus_library`)
- Spring WebFlux + Reactor + MongoDB Reactive (`movies-info-service`, `RM01-01-reactor-basics`)
- Spring WebFlux + R2DBC + PostgreSQL + gRPC / Protobuf (`movies-review-service`)
- [tus-js-client](https://github.com/tus/tus-js-client) di sisi browser

## Prasyarat

- JDK 21
- Maven (atau gunakan wrapper `mvnw` yang sudah disertakan) untuk project Maven
- Gradle (atau gunakan wrapper `gradlew` yang sudah disertakan) untuk project Gradle
- Docker (opsional) untuk menjalankan MongoDB dan PostgreSQL lewat `compose-reactive.yml`
- MongoDB untuk menjalankan `movies-info-service`
- PostgreSQL untuk menjalankan `movies-review-service`

## Cara Menjalankan

Masuk ke folder project yang diinginkan, lalu jalankan:

```bash
# Project Maven (tus-manual, tus_library)
./mvnw spring-boot:run

# Project Gradle (movies-info-service, movies-review-service, RM01-01-reactor-basics)
./gradlew bootRun
```

Di Windows gunakan `mvnw.cmd` atau `gradlew.bat`.

Untuk project upload (`tus-manual` dan `tus_library`), buka halaman `http://localhost:8080/upload` setelah aplikasi berjalan.

### Database (MongoDB & PostgreSQL)

Database untuk project reaktif disediakan lewat Docker Compose:

```bash
# 1. Salin contoh env, lalu sesuaikan nilainya (file ini di-ignore git)
cp .spring-boot-reactive-env.example.example .spring-boot-reactive-env

# 2. Jalankan MongoDB (27017) dan PostgreSQL (5433)
docker compose --env-file .spring-boot-reactive-env -f compose-reactive.yml up -d
```

`movies-review-service` membaca `POSTGRES_DB`, `POSTGRES_USER`, dan `POSTGRES_PASSWORD` dari environment, jadi export dulu sebelum `bootRun`:

```bash
set -a; source ../.spring-boot-reactive-env; set +a
./gradlew bootRun
```

Port default: `movies-review-service` REST di `8081` (gRPC memakai port default Spring gRPC).

## Catatan

Kode di repository ini dibuat untuk keperluan belajar dan demonstrasi pada artikel, bukan untuk langsung dipakai di production tanpa penyesuaian (keamanan, validasi, konfigurasi, dsb.).

## Artikel

Daftar lengkap artikel: **https://sch39.dev/articles?tag=java-spring-boot**
