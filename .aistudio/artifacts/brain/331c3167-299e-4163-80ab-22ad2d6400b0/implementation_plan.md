# Rencana Implementasi: Fitur Impor E-Statement Livin' Mandiri & Mesin Pembelajar Pola Finansial (Visual Identity: #121212 & #1F2937)

Dokumen ini merinci arsitektur, diagram alur Mermaid, dan strategi implementasi untuk fitur **Impor E-Statement Livin' by Mandiri**, **Mesin Pembelajar Pola Finansial (Pattern Learner)**, **Modul Pemecah Tarik Tunai (Cash Splitter)**, dan **Dashboard Keputusan Finansial (50/30/20 & Deteksi Kebocoran)** dengan menerapkan Palet Desain Khusus:
- **Background Utama**: `#121212` (OLED Deep Charcoal)
- **Kontainer & Kartu (Cards)**: `#1F2937` (Dark Slate Surface)
- **Tombol & Aksi Utama (CTA / Glowing)**: Emerald Glow `#10B981`
- **Sorotan & Warning (Accent)**: Amber `#F59E0B`

---

## 1. Diagram Alur Pengguna (User Flow)

```mermaid
flowchart TD
    Start([Buka CatatKas]) --> ModeCheck{Pilih Sumber Data}
    
    ModeCheck -->|E-Statement Livin'| ImportFlow[Pilih Berkas PDF / CSV Mandiri]
    ModeCheck -->|Share Struk / OCR| ReceiptFlow[Pindai Struk / Bukti Bayar QRIS]
    ModeCheck -->|Input Cepat| ManualFlow[Input Transaksi Manual]
    
    ImportFlow --> PasswordCheck{Perlu Sandi Tanggal Lahir?}
    PasswordCheck -->|Ya| InputPIN[Masukkan DDMMYYYY / Gunakan Sandi Tersimpan]
    PasswordCheck -->|Tidak / Terbuka| ParsingEngine[Parsing Mutasi On-Device]
    InputPIN --> ParsingEngine
    
    ParsingEngine --> Classification[Pattern Learner: Kategorisasi Cerdas Otomatis]
    Classification --> SpecialCase{Ada Tarik Tunai ATM?}
    
    SpecialCase -->|Ya| CashSplit[Buka Modal Pemecah Tarik Tunai]
    CashSplit --> PreviewScreen[Tinjau Hasil Ekstraksi Mutasi]
    SpecialCase -->|Tidak| PreviewScreen
    
    PreviewScreen --> ConfirmImport[Konfirmasi Simpan ke Dompet Terpilih]
    ConfirmImport --> UpdateBalance[(Update Saldo Dompet & Database)]
    UpdateBalance --> DecisionDashboard[Buka Dashboard Finansial 50/30/20]
```

---

## 2. Arsitektur Topologi Sistem (System Architecture)

```mermaid
graph TB
    subgraph UI_Layer [Presentation Layer - Jetpack Compose]
        ScreenTransactions[TransactionsScreen]
        ScreenStats[StatisticsScreen & 50/30/20]
        ScreenCategories[CategoriesScreen]
        ScreenSettings[SettingsScreen]
        DialogImport[StatementImportModal]
        DialogCashSplit[CashSplitModal]
    end

    subgraph ViewModel_Layer [State & ViewModel Layer]
        MainVM[MainViewModel]
        StateTrans[StateFlow: Transactions & Wallets]
        StateDecision[StateFlow: FinancialDecisionMetrics]
    end

    subgraph Engine_Layer [Domain & On-Device Processing Engine]
        MandiriParser[MandiriStatementParser]
        PatternEngine[PatternLearnerEngine]
        DecisionEngine[FinancialDecisionEngine]
        ExportUtil[ReportExporter & DataBackupManager]
    end

    subgraph Data_Layer [Persistence & Data Layer]
        RepoFinance[FinanceRepository]
        RepoPrefs[PreferencesRepository]
        DB[(Room Database: AppDatabase)]
        DataStore[(Jetpack DataStore Preferences)]
    end

    UI_Layer --> ViewModel_Layer
    ViewModel_Layer --> Engine_Layer
    ViewModel_Layer --> Data_Layer
    Engine_Layer --> Data_Layer
    RepoFinance --> DB
    RepoPrefs --> DataStore
```

---

## 3. Diagram Alur Mesin Pembelajar Pola (Pattern Learner Engine)

```mermaid
flowchart LR
    Mutation[Item Mutasi Masuk] --> TextClean[Pembersihan Teks & Normalisasi]
    TextClean --> RuleCheck{Cek Rekening Internal?}
    
    RuleCheck -->|Mandiri ke GoPay/OVO| WalletTransfer[Catat Pemindahan Saldo Otomatis]
    RuleCheck -->|Tarik Tunai CRM/ATM| CashMarker[Tandai untuk Cash Splitter]
    RuleCheck -->|Bukan Transfer Internal| KnowledgeLookup{Cek Kamus Merchant}
    
    KnowledgeLookup -->|Ditemukan| HighConfidence[Kategori Terpilih - Bobot 0.95]
    KnowledgeLookup -->|Belum Ada di Kamus| Heuristic[Analisis Kata Kunci QRIS / Indomaret / PLN / Gaji]
    
    Heuristic --> UserConfirm[Tampilkan Rekomendasi di UI]
    UserConfirm --> UserAdjust{User Mengubah Kategori?}
    UserAdjust -->|Ya| LearnPattern[Update MerchantKnowledgeEntity: Bobot Naik +0.1]
    UserAdjust -->|Tidak| KeepPattern[Simpan Transaksi dengan Kategori Usulan]
```

---

## 4. Diagram Entitas Basis Data (Entity Relationship)

```mermaid
erDiagram
    WALLETS ||--o{ TRANSACTIONS : contains
    CATEGORIES ||--o{ TRANSACTIONS : classifies
    TRANSACTIONS ||--o{ CASH_BREAKDOWNS : splits_into
    CATEGORIES ||--o{ CASH_BREAKDOWNS : categorizes
    CATEGORIES ||--o{ MERCHANT_KNOWLEDGE : trains

    WALLETS {
        long id PK
        string name
        string type
        double balance
        string iconKey
        string colorHex
    }

    CATEGORIES {
        long id PK
        string name
        string type
        string iconKey
        string colorHex
        boolean isDefault
        int orderIndex
    }

    TRANSACTIONS {
        long id PK
        double amount
        string type
        long categoryId FK
        long walletId FK
        long destinationWalletId
        long dateMillis
        string note
        string rawDescription
        boolean isAtmWithdrawal
    }

    CASH_BREAKDOWNS {
        long id PK
        long parentTransactionId FK
        double amount
        long categoryId FK
        string note
    }

    MERCHANT_KNOWLEDGE {
        string keyword PK
        long categoryId FK
        float confidenceWeight
        long usageCount
    }
```

---

## 5. Sistem Identitas Visual & Palette Warna

```mermaid
graph LR
    subgraph ColorTokens [Palet Warna Sesuai Permintaan User]
        BG["#121212 - Background Utama OLED Dark"]
        Card["#1F2937 - Surface Card & Container Slate"]
        CTA["#10B981 - Emerald Glow CTA / Sukses"]
        Accent["#F59E0B - Amber Warning & Rating Highlight"]
    end
```

- **Latar Belakang (`#121212`)**: Mengurangi kelelahan mata, hemat daya layar AMOLED, dan memberikan kedalaman visual kontras tinggi.
- **Kontainer & Kartu (`#1F2937`)**: Warna Slate gelap yang lembut untuk membedakan kartu ringkasan saldo, daftar transaksi, dan modal formulir.
- **Emerald Glow (`#10B981`)**: Aksen menyala untuk tombol catat transaksi baru, konfirmasi impor mutasi, status saldo surplus, dan tab aktif.
- **Amber Accent (`#F59E0B`)**: Sorotan untuk kartu kebocoran anggaran (spending leak), status transaksi pending, serta peringatan batas 50/30/20.

---

## 6. Rencana Implementasi & Verifikasi Kode

1. **Sinkronisasi Desain Visual**: Memastikan warna `#121212`, `#1F2937`, `#10B981`, dan `#F59E0B` diterapkan secara presisi di seluruh elemen `Theme.kt`, `Color.kt`, dan kartu UI.
2. **Komponen Modal Impor E-Statement**: Menyediakan UI interaktif untuk simulasi atau pemilihan file mutasi Mandiri, dialog sandi tanggal lahir (DDMMYYYY), dan pratinjau daftar transaksi yang terdeteksi secara otomatis.
3. **Penyatuan ke Navigation & Main Screen**: Mengintegrasikan tombol aksi cepat di layar Transaksi dan Statistik untuk mengakses modul impor dan analisis 50/30/20.
4. **Verifikasi Build**: Melakukan kompilasi `compile_applet` untuk menjamin tidak ada syntax error atau ketergantungan yang rusak.
