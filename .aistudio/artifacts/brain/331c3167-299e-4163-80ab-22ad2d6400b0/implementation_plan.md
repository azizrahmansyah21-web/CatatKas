# QA Test Case Matrix & Mobile Performance Benchmark Plan: CatatKas Android

Dokumen ini merupakan spesifikasi pengujian komprehensif berstandar **Senior QA Engineering** untuk aplikasi mobile **CatatKas**, mencakup matriks pengujian fungsional seluruh modul, penanganan skenario ekstrem/edge cases, serta protokol benchmark performa (Cold/Warm Startup, Frame Drop / Jank, Memory Leak, dan efisiensi konsumsi daya).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Sesuai arahan klarifikasi yang telah dikonfirmasi:
> - **Cakupan Pengujian**: Menyeluruh (Fungsional, Performa, dan Benchmark).
> - **Fokus Utama Benchmark**: *Startup Time*, *Frame Drop (UI Jank)*, dan *Konsumsi Memori (RAM / Heap)*.
> - **Format Keluaran**: Matriks Test Case terstruktur dengan parameter QA Senior (Pre-conditions, Test Steps, Expected Results, Severity, Priority) serta tabel Benchmark KPI kuantitatif.
> - **Batasan Eksekusi**: Dokumen ini berfokus murni pada arsitektur pengujian, skenario test case, dan metodologi benchmark QA tanpa melakukan perubahan liar pada source code aplikasi.

---

## 1. QA Strategy & Testing Scope

### 1.1 Piramida Pengujian & Metodologi
Sistem pengujian CatatKas mengadopsi pendekatan holistik yang membagi validasi ke dalam 4 tingkatan:
1. **Core Business Logic Validation**: Memastikan akurasi matematis Financial Decision Engine, aturan 50/30/20, formula skor keputusan, dan kalkulasi saldo.
2. **Component & Integration Testing**: Memastikan interaksi DAO Room, ViewModel StateFlow, dan konversi format berkas (Excel/CSV/OCR).
3. **End-to-End Functional Journey**: Menguji alur pengguna nyata (User Journeys) dari onboarding, transaksi, hingga realisasi wishlist.
4. **Non-Functional Performance Benchmarking**: Pengujian kuantitatif latensi, stabilitas rendering 60/120 FPS, dan profil konsumsi memori di bawah beban data tinggi.

```
                  ┌────────────────────────────────────────┐
                  │       Non-Functional Benchmark        │
                  │ (Startup, Jank/FPS, Memory, Profiling) │
                  ├────────────────────────────────────────┤
                  │         End-to-End User Journeys       │
                  │  (Transaksi, OCR, Impor Excel, Rencana)│
                  ├────────────────────────────────────────┤
                  │     Integration & State Management     │
                  │  (Room SQLite, ViewModel, Coroutines)  │
                  ├────────────────────────────────────────┤
                  │       Unit & Core Engine Logic         │
                  │  (DecisionEngine, BudgetRules, Math)   │
                  └────────────────────────────────────────┘
```

---

## 2. Test Environment & Matrix Device Profiles

Pengujian dilakukan pada 3 tingkatan spesifikasi perangkat Android untuk menjamin konsistensi pengalaman pengguna:

| Device Tier | Kategori Spesifikasi | Contoh Perangkat Acuan | Target OS | RAM / Storage |
| :--- | :--- | :--- | :--- | :--- |
| **Tier 1 (Low-End)** | Budget / Entry-level | Redmi 9A / Samsung Galaxy A04 | Android 10 - 12 (Go/Standard) | 2GB - 3GB RAM |
| **Tier 2 (Mid-Range)** | Daily Driver Standar | Samsung Galaxy A54 / Poco X5 | Android 13 - 14 | 6GB - 8GB RAM |
| **Tier 3 (High-End)** | Flagship Device | Google Pixel 8 / Samsung S24 | Android 14 - 15 | 8GB - 12GB RAM (120Hz) |

---

## 3. Comprehensive QA Test Case Matrix

### Modul A: Autentikasi, Profil & Pengaturan
| Test ID | Skenario Pengujian | Pre-conditions | Langkah Pengujian (Test Steps) | Hasil yang Diharapkan (Expected Result) | Severitas | Prioritas |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-AUTH-001** | First-Launch Onboarding & Profil Default | Aplikasi baru dipasang (Fresh install) | 1. Buka aplikasi.<br>2. Verifikasi layar awal dan tab Profil.<br>3. Periksa status login default. | Pengguna dapat masuk secara seamless; data profil tamu/lokal terinisialisasi tanpa crash. | High | P1 |
| **TC-AUTH-002** | Google Sign-In via Credential Manager | Akun Google aktif pada perangkat | 1. Masuk ke tab Profil.<br>2. Klik tombol 'Masuk dengan Google'.<br>3. Pilih akun Google valid. | Bottom sheet Credential Manager muncul; setelah autentikasi sukses, nama & email pengguna terupdate di header profil. | Critical | P1 |
| **TC-AUTH-003** | Pembatalan Dialog Google Sign-In | Dialog Credential Manager terbuka | 1. Ketuk di luar dialog atau tekan tombol back.<br>2. Batalkan autentikasi. | Dialog menutup dengan mulus; aplikasi tetap berada pada kondisi profil sebelumnya tanpa error state tak terduga. | Medium | P2 |
| **TC-AUTH-004** | Pengaturan Target Gaji / Jam Kerja | Berada di menu Pengaturan Keuangan | 1. Buka Pengaturan di tab Profil.<br>2. Masukkan pendapatan per jam (misal Rp 50.000).<br>3. Simpan perubahan. | Nilai tersimpan ke Preferences/DataStore; kalkulator nilai kerja pada Rencana Kas langsung menggunakan tarif baru. | High | P1 |

---

### Modul B: Manajemen Transaksi & Filter Kategori Terpadu
| Test ID | Skenario Pengujian | Pre-conditions | Langkah Pengujian (Test Steps) | Hasil yang Diharapkan (Expected Result) | Severitas | Prioritas |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-TRX-001** | Pencatatan Pengeluaran Standar | Berada di layar Transaksi | 1. Tekan tombol FAB Tambah Transaksi.<br>2. Pilih tipe 'Pengeluaran'.<br>3. Masukkan nominal Rp 75.000.<br>4. Pilih kategori 'Makanan' & pos 'Kebutuhan'.<br>5. Simpan transaksi. | Transaksi langsung tercatat di daftar; saldo berkurang Rp 75.000; animasi pembaruan list berjalan lancar. | Critical | P1 |
| **TC-TRX-002** | Pencatatan Pemasukan & Update Saldo | Saldo awal diketahui | 1. Buka BottomSheet Transaksi.<br>2. Pilih tipe 'Pemasukan'.<br>3. Input nominal Rp 5.000.000, kategori 'Gaji'.<br>4. Simpan transaksi. | Total saldo bersih bertambah Rp 5.000.000; ringkasan arus kas bulanan ter-update otomatis secara reaktif. | Critical | P1 |
| **TC-TRX-003** | Filter Kategori via Horizontal Chip Bar | Terdapat beragam transaksi pada database | 1. Pada layar Transaksi, klik chip kategori tertentu (misal: 'Transportasi').<br>2. Amati daftar transaksi. | Daftar transaksi hanya menampilkan item kategori 'Transportasi'; chip aktif memiliki indikator border glow / visual aktif. | High | P1 |
| **TC-TRX-004** | Filter Tipe Transaksi (Semua / Pengeluaran / Pemasukan) | Transaksi mix tersimpan | 1. Klik chip 'Pengeluaran'.<br>2. Amati hasil list.<br>3. Klik chip 'Pemasukan'. | List terfilter instan (<50ms); badge jumlah transaksi yang sesuai terhitung tepat. | High | P1 |
| **TC-TRX-005** | Pencarian Transaksi Real-time (Search Query) | Lebih dari 20 transaksi ada di list | 1. Buka kolom pencarian.<br>2. Ketik kata kunci catatan (misal: "Kopi"). | List tersaring secara instan (debounced); menampilkan transaksi yang cocok dengan deskripsi atau merchant. | Medium | P2 |
| **TC-TRX-006** | Hapus Transaksi & Revert Saldo | Transaksi ada di list | 1. Buka detail transaksi atau swipe hapus.<br>2. Konfirmasi penghapusan. | Transaksi terhapus dari Room Database; saldo dan rekap statistik bulanan ter-recalculate seketika. | High | P1 |

---

### Modul C: Rencana Kas & Financial Decision Engine
| Test ID | Skenario Pengujian | Pre-conditions | Langkah Pengujian (Test Steps) | Hasil yang Diharapkan (Expected Result) | Severitas | Prioritas |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-PLAN-001** | Kalkulasi Alokasi Anggaran 50/30/20 | Total pemasukan bulan ini terdata | 1. Buka tab 'Rencana Kas'.<br>2. Periksa kartu Budget Rule 50/30/20.<br>3. Verifikasi alokasi: Needs (50%), Wants (30%), Savings (20%). | Nominal alokasi tepat secara matematis; progress bar menampilkan proporsi persentase akurat. | High | P1 |
| **TC-PLAN-002** | Simulasi "Beli Sekarang vs Nanti" (Kalkulator Jam Kerja) | Jam kerja per jam diset Rp 40.000 | 1. Masukkan nominal barang Rp 800.000.<br>2. Pilih tingkat urgensi (Rendah/Tinggi).<br>3. Tekan 'Hitung Keputusan'. | Menampilkan ekuivalen 20 jam kerja; rekomendasi jeda waktu (Cooling-off) sesuai urgensi; visual badge ditampilkan. | High | P1 |
| **TC-PLAN-003** | Penambahan Item Wishlist dengan Status Cooling-Off | Layar Rencana Kas aktif | 1. Klik 'Tambah Rencana Belanja'.<br>2. Masukkan judul barang, harga estimasi, tipe (Keinginan).<br>3. Simpan rencana. | Item masuk ke daftar rencana dengan status 'PENDING'; penghitung mundur jeda 72 jam terinisialisasi. | High | P1 |
| **TC-PLAN-004** | Realisasi Wishlist Menjadi Transaksi Pengeluaran | Ada item wishlist pending | 1. Buka kartu rencana barang.<br>2. Klik tombol 'Realisasikan Jadi Transaksi'.<br>3. Konfirmasi pemilihan dompet. | Status item berubah menjadi 'REALIZED'; otomatis terkonversi menjadi entitas transaksi pengeluaran baru di database. | Critical | P1 |
| **TC-PLAN-005** | Evaluasi Decision Score (Smart Spending Algorithm) | Berada di form rencana belanja | 1. Masukkan barang mewah harga tinggi dengan urgensi rendah.<br>2. Amati skor evaluasi. | Sistem memberi skor rendah / peringatan impulsif; warna indikator kuning/merah dengan insight saran keuangan. | Medium | P2 |

---

### Modul D: Pindai Struk (OCR) & Impor Berkas E-Statement
| Test ID | Skenario Pengujian | Pre-conditions | Langkah Pengujian (Test Steps) | Hasil yang Diharapkan (Expected Result) | Severitas | Prioritas |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-SCAN-001** | Pindai Struk Fisik via Kamera | Izin kamera telah diberikan | 1. Buka menu 'Pindai Struk'.<br>2. Pilih 'Ambil Foto Langsung'.<br>3. Ambil gambar struk kasir toko ritel.<br>4. Proses OCR. | Gambar berhasil dicapture; total belanja dan tanggal terdeteksi otomatis dan terisi ke form transaksi. | High | P1 |
| **TC-SCAN-002** | Unggah Tangkapan Layar Pembayaran dari Galeri | File screenshot e-wallet (GoPay/OVO/BCA) ada di galeri | 1. Pilih 'Pilih dari Galeri'.<br>2. Pilih screenshot bukti transfer/pembayaran QRIS. | Sistem mem-parsing nominal pembayaran dan nama penerima/merchant secara akurat; pengguna tinggal konfirmasi simpan. | High | P1 |
| **TC-SCAN-003** | Impor Berkas Excel (.xlsx / .xls) Standar | File e-statement bank format Excel tersedia | 1. Di bagian Statistik/Impor, pilih 'Impor Berkas Excel'.<br>2. Pilih berkas .xlsx.<br>3. Jalankan parser impor. | Semua baris transaksi ter-ekstrak; tanggal, deskripsi, dan nominal debit/kredit terpetakan rapi ke tabel preview sebelum commit. | Critical | P1 |
| **TC-SCAN-004** | Impor Berkas Excel Terproteksi Password (Encrypted) | Berkas e-statement Excel bank ber-password | 1. Pilih berkas Excel terenkripsi.<br>2. Sistem memunculkan prompt password.<br>3. Input password yang tepat.<br>4. Submit. | Berkas berhasil didekripsi di memori lokal; data transaksi terurai tanpa kebocoran password atau crash. | Critical | P1 |
| **TC-SCAN-005** | Impor Berkas Format CSV | Berkas CSV transaksi bank tersedia | 1. Pilih 'Impor Berkas CSV'.<br>2. Pilih file CSV dengan pemisah koma/titik koma.<br>3. Lanjutkan impor. | Sistem mengenali delimiter otomatis; seluruh transaksi tersimpan ke Room database dengan flag status sukses. | High | P1 |
| **TC-SCAN-006** | Penanganan Berkas Rusak / Format Corrupt (Negative Test) | Berkas biner non-Excel yang di-rename .xlsx | 1. Pilih berkas corrupt.<br>2. Klik proses impor. | Menampilkan dialog pesan kesalahan ramah pengguna: "Format berkas tidak valid atau rusak"; aplikasi tidak force-close (ANR/Crash). | High | P1 |

---

### Modul E: Edge Cases, Ketahanan & Offline Reliability
| Test ID | Skenario Pengujian | Pre-conditions | Langkah Pengujian (Test Steps) | Hasil yang Diharapkan (Expected Result) | Severitas | Prioritas |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-EDGE-001** | Zero State / Empty State Baru Install | Database kosong | 1. Buka tab Transaksi, Rencana Kas, dan Statistik berturut-turut. | Setiap tab menampilkan ilustrasi empty state yang informatif beserta tombol ajakan aksi (CTA) yang jelas. | Medium | P2 |
| **TC-EDGE-002** | Transaksi Nominal Ekstrem (Boundary Value) | Form transaksi terbuka | 1. Masukkan nominal Rp 999.999.999.999 (triliun).<br>2. Simpan transaksi.<br>3. Amati tampilan kartu. | Format Rupiah rapi (Rp 999,99 Miliar / Triliun); tidak terjadi overflow teks, pemotongan layout, ataupun integer overflow di DAO. | High | P1 |
| **TC-EDGE-003** | Rotasi Layar (Configuration Change) | Sedang mengisi form input transaksi / kalkulator rencana | 1. Isi input 50%.<br>2. Putar orientasi layar dari Portrait ke Landscape lalu kembali ke Portrait. | State input form tidak hilang (preserved via rememberSaveable / ViewModel); keyboard menutup atau menyesuaikan dengan benar. | Medium | P2 |
| **TC-EDGE-004** | Mode Pesawat / Tanpa Koneksi Internet (Offline-First) | Koneksi internet dimatikan (Airplane mode ON) | 1. Lakukan pembuatan transaksi, update rencana belanja, kalkulasi keputusan. | Semua fungsi berjalan normal 100% lokal tanpa error banner atau loading tak terhingga (Room offline-first). | Critical | P1 |
| **TC-EDGE-005** | Stress Testing 10.000 Transaksi Tersimpan | Seed database dengan 10.000 row transaksi | 1. Buka layar Transaksi.<br>2. Lakukan fast-scrolling LazyColumn dari atas ke bawah.<br>3. Terapkan filter kategori. | List me-render secara lazy tanpa kehabisan memori (OOM); scroll tetap mulus dan filter selesai dalam < 200ms. | Critical | P1 |

---

## 4. Mobile App Performance Benchmark Standards & KPIs

Standar metrik di bawah ini dirancang mengacu pada standar **Android Vitals Google Play & Senior Mobile QA Benchmarks**:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        BENCHMARK KPI OVERVIEW - CATATKAS                               │
├──────────────────────────┬──────────────────────────┬──────────────────────────────────┤
│ Startup Time             │ Rendering & Jank         │ Memory & Resources               │
│ • Cold Start: < 1.200 ms │ • Frame Jank: < 2.0%     │ • Baseline RAM: < 95 MB          │
│ • Warm Start: < 500 ms   │ • 60 FPS Target: 16.6 ms │ • Peak Memory: < 180 MB          │
│ • Hot Start:  < 200 ms   │ • 120 FPS Target: 8.3 ms │ • Zero Memory Leak (LeakCanary)  │
└──────────────────────────┴──────────────────────────┴──────────────────────────────────┘
```

### 4.1 Latensi Startup Aplikasi (App Startup Benchmarks)

Diukur menggunakan **Jetpack Macrobenchmark** (`StartupTimingMetric`) dan ADB Profiling:

| Metrik Startup | Deskripsi Kondisi | Ambang Batas Ideal (Target) | Ambang Batas Kritis (Threshold) | Prosedur Pengukuran QA |
| :--- | :--- | :--- | :--- | :--- |
| **Cold Start** | Proses belum berjalan di memori; OS membuat proses baru dari awal hingga layar pertama ter-render (`reportFullyDrawn`). | **< 1.000 ms (P50)**<br>**< 1.300 ms (P90)** | **> 2.000 ms** (Google Vitals Bad Behavior) | `adb shell am start-activity -W -n com.aistudio.weathertracker.kxmpzq/com.example.MainActivity` (rata-rata 10x iterasi setelah reboot). |
| **Warm Start** | Aktivitas harus dibuat ulang, namun proses aplikasi masih ada di background cache OS. | **< 350 ms (P50)**<br>**< 500 ms (P90)** | **> 800 ms** | Tekan tombol Home, tunggu 5 menit, buka kembali aplikasi melalui Recent Apps. |
| **Hot Start** | Aktivitas dan proses aplikasi masih berada di foreground / memori aktif, hanya dipanggil kembali. | **< 150 ms (P50)**<br>**< 200 ms (P90)** | **> 300 ms** | Buka aplikasi lain, lalu segera beralih kembali ke CatatKas. |

---

### 4.2 Kualitas Rendering & Frame Drops (UI Jank Benchmark)

Diukur menggunakan `FrameTimingMetric` pada layar kompleks (Layar Transaksi dengan 1.000 item dan Layar Rencana Kas):

| Metrik Rendering | Deskripsi | Standar Senior QA | Batas Toleransi Maksimal | Metode Pengujian |
| :--- | :--- | :--- | :--- | :--- |
| **Frame Jank Rate** | Persentase frame yang membutuhkan waktu render lebih dari 16.6ms (pada layar 60Hz) atau 8.3ms (pada 120Hz). | **< 1.5% dari total frame** | **Maksimal 3.0%** | `adb shell dumpsys gfxinfo com.aistudio.weathertracker.kxmpzq framestats` selama 60 detik fling scroll terus-menerus. |
| **Slow UI Frames (>50ms)** | Frame yang mengalami lag parah (terasa tersendat kasat mata oleh mata pengguna). | **0 frame per 1000 frame** | **< 2 frame per 1000 frame** | Deteksi automated via Macrobenchmark trace profiling. |
| **Frozen Frames (>700ms)** | Frame yang membeku lebih dari 0.7 detik, rentan memicu dialog ANR (App Not Responding). | **0 frame (0.00%)** | **0 frame** | Logcat filtering untuk `Skipped ... frames!` dan pemantauan ANR Watchdog. |

---

### 4.3 Profiling Konsumsi Memori & Garbage Collection (RAM Benchmark)

Diukur dengan **Android Studio Memory Profiler** dan uji ketahanan kebocoran (Leak Testing):

| Parameter Memori | Baseline / Idle | Beban Berat (Scrolling 5.000 Item) | Pemrosesan File (Excel Impor / OCR) | Kriteria Kelulusan QA (Pass/Fail) |
| :--- | :--- | :--- | :--- | :--- |
| **Java/Kotlin Heap** | < 45 MB | < 85 MB | < 120 MB | **PASS** jika heap kembali turun (GC stabil) setelah proses selesai. |
| **Native Memory** | < 35 MB | < 60 MB | < 90 MB | **PASS** jika tidak ada kebocoran bitmap kamera/gambar struk. |
| **Total RSS / PSS** | < 90 MB | < 160 MB | < 210 MB | **FAIL** jika terjadi OOM (Out Of Memory) atau crash saat file berukuran 10MB diimpor. |
| **GC Pause Frequency** | < 1x per 30 detik | < 3x saat scroll intensif | Normal saat parsing | **PASS** jika durasi GC pause tidak pernah melampaui 12ms. |
| **Activity/ViewModel Leaks** | 0 Leak | 0 Leak | 0 Leak | **PASS** jika LeakCanary tidak mendeteksi retained reference setelah rotasi layar 10x. |

---

### 4.4 Efisiensi Penyimpanan Database & Query Latency (I/O Benchmark)

Diukur pada basis data Room SQLite yang telah terisi **5.000 transaksi**:

| Operasi Basis Data | Target Latency (P50) | Target Latency (P95) | Strategi Optimasi QA |
| :--- | :--- | :--- | :--- |
| **Insert Transaksi Tunggal** | < 8 ms | < 18 ms | Eksekusi di background thread (`Dispatchers.IO`) dengan index pada foreign key kategori. |
| **Bulk Insert 500 Transaksi (Excel Import)** | < 120 ms | < 250 ms | Penggunaan `@Transaction` atomic batch insert pada Room DAO. |
| **Query Filter Kategori + Paging (30 item)** | < 12 ms | < 25 ms | Pemilihan index query pada kolom `categoryId`, `year`, dan `month`. |
| **Perhitungan Agregat Saldo & 50/30/20** | < 15 ms | < 30 ms | Query Room `SUM(amount)` langsung di tingkat SQLite tanpa kalkulasi manual iteratif di memori. |

---

## 5. Benchmark Execution Protocols & Tooling Guide

### 5.1 Perangkat Tooling yang Digunakan
1. **Android Studio Profiler**: Untuk analisis real-time CPU spikes, alokasi Heap memory, dan inspeksi Bitmap leak saat pemindaian struk.
2. **Jetpack Macrobenchmark**: Untuk benchmarking deterministik tanpa interferensi JIT compiler menggunakan target build release.
3. **LeakCanary 2.x**: Untuk audit otomatis retained memory pada navigasi bottom-bar berulang kali.
4. **ADB Gfxinfo & Systrace / Perfetto**: Untuk penangkapan frame rendering trace dan audit frame drop secara presisi.

### 5.2 Skrip Perintah Eksekusi Cepat (CLI QA Toolkit)

```bash
# 1. Mengukur Waktu Startup (Cold Start Benchmark)
adb shell am force-stop com.aistudio.weathertracker.kxmpzq
adb shell am start-activity -W -n com.aistudio.weathertracker.kxmpzq/com.example.MainActivity

# 2. Mengukur Rendering Frame & Jank Rate
adb shell dumpsys gfxinfo com.aistudio.weathertracker.kxmpzq reset
# Lakukan interaksi scroll pada daftar transaksi selama 30 detik, kemudian jalankan:
adb shell dumpsys gfxinfo com.aistudio.weathertracker.kxmpzq

# 3. Mengukur Konsumsi Memori (PSS/RSS Snapshot)
adb shell dumpsys meminfo com.aistudio.weathertracker.kxmpzq

# 4. Stress Test UI Stability (Monkey Testing 5.000 Events)
adb shell monkey -p com.aistudio.weathertracker.kxmpzq --pct-touch 70 --pct-motion 20 --pct-appswitch 10 -v 5000
```

---

## 6. Kriteria Rilis QA (Sign-off Acceptance Criteria)

Aplikasi CatatKas dinyatakan **SIAP RILIS (PRODUCTION READY)** apabila memenuhi syarat mutlak berikut:
- [ ] **Zero Crash / ANR Rate**: Crash-free sessions > 99.9% dan ANR rate < 0.05% pada seluruh skenario pengujian.
- [ ] **100% Modul Fungsional Lulus**: Semua test case P1 (Prioritas 1) berstatus **PASS** tanpa pengecualian.
- [ ] **Cold Start < 1.300 ms** pada perangkat Mid-range (Tier 2).
- [ ] **Frame Jank Rate < 2.0%** saat scrolling cepat 1.000 transaksi.
- [ ] **Zero Memory Leak** terkonfirmasi pada alur pembukaan kamera struk dan impor berkas Excel.
- [ ] Penanganan graceful error pada seluruh input berkas rusak atau berpassword salah.
