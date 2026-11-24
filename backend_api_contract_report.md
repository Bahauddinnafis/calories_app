# Backend API Contract & Gap Report (Calories App v3)

Laporan ini menyajikan pemetaan integrasi backend API eksisting yang dipertahankan, usulan penyesuaian payload DTO, serta daftar endpoint backend baru yang saat ini masih menggunakan Local Room Database / DataStore / Fake Repositories selama pengembangan paralel UI/UX Material Design 3 (v3).

---

## 1. Endpoint Eksisting yang Dipertahankan

Seluruh kontrak pemanggilan endpoint Retrofit eksisting di `CaloriesBackendApi.kt` dipertahankan dan dihubungkan ke UI v3 tanpa mengubah kontrak aktif:

| HTTP Method | Endpoint Path | Layar / Modul Pemanggil | Fungsi Utama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/health` | Network Health Check / App Launch | Memeriksa ketersediaan dan konektivitas server backend. |
| `POST` (Multipart) | `/api/v1/scans` | `MultiModeScannerScreen` (Real-time, Take Photo, Gallery) | Mengirim foto makanan dari kamera/galeri untuk dianalisis oleh AI Cloud. |
| `POST` | `/api/v1/scans/{scanId}/recalculate` | `CorrectionScreen` (Human-in-the-Loop) | Meminta rekalkulasi nutrisi jika pengguna mengoreksi nama makanan atau gramasi bahan. |

---

## 2. Penyesuaian Payload (Usulan Field Baru pada Endpoint Eksisting)

Untuk mendukung visualisasi komponen bahan terdeteksi dan catatan gizi kontekstual pada `ResultScreen` v3, direkomendasikan penambahan field opsional pada JSON response `POST /api/v1/scans`:

### Proposed Response Gap (POST `/api/v1/scans` Response)
```json
{
  "scan_id": "scan_20261124_9823",
  "status": "completed",
  "meal_name": "Paket Nasi Padang Ayam Pop",
  "overall_confidence": 0.964,
  "ai_notice_text": "Kandungan protein 38g sangat mendukung target harian Anda. Imbangi dengan air putih yang cukup.",
  "ingredients": [
    {
      "food_code": "ID_RICE_WHITE",
      "food_name": "Nasi Putih Kukus",
      "estimated_weight_g": 150.0,
      "nutrition": {
        "energy_kcal": 195.0,
        "protein_g": 4.1,
        "carbohydrate_g": 42.5,
        "fat_g": 0.4
      }
    },
    {
      "food_code": "ID_CHICKEN_POP",
      "food_name": "Ayam Pop (Tanpa Kulit)",
      "estimated_weight_g": 120.0,
      "nutrition": {
        "energy_kcal": 265.0,
        "protein_g": 30.2,
        "carbohydrate_g": 1.2,
        "fat_g": 15.6
      }
    }
  ],
  "total_nutrition": {
    "energy_kcal": 640.0,
    "protein_g": 38.0,
    "carbohydrate_g": 74.0,
    "fat_g": 21.0
  }
}
```

---

## 3. Endpoint Baru yang Perlu Dibuat di Backend

Layar fitur v3 berikut saat ini menggunakan penyimpanan lokal (Room SQLite & SharedPreferences) dan Fake Repositories. Backend disarankan membuat endpoint berikut untuk sinkronisasi cloud akun pengguna:

### A. Endpoint Profiling Onboarding Pengguna

#### `POST /api/v1/user/profile/onboarding`
- **Tujuan**: Menyimpan hasil kuesioner 5-step onboarding pengguna ke database backend.
- **Request Body**:
```json
{
  "sex": "male",
  "age_years": 25,
  "height_cm": 170.0,
  "weight_kg": 72.0,
  "target_weight_kg": 60.0,
  "weekly_pace_kg": 0.50,
  "goal": "lose",
  "activity_level": "light",
  "dietary_preferences": ["Masakan Nusantara", "Tinggi Protein", "Halal"],
  "allergies": ["Makanan Laut (Seafood)"],
  "schedules": {
    "breakfast_time": "07:30",
    "lunch_time": "12:30",
    "dinner_time": "19:00"
  },
  "settings": {
    "hydration_goal_enabled": true,
    "calorie_warning_enabled": true
  }
}
```
- **Response Body (200 OK)**:
```json
{
  "status": "success",
  "user_id": "usr_882031",
  "calculated_targets": {
    "bmr_kcal": 1664.0,
    "tdee_kcal": 2288.0,
    "daily_energy_kcal": 1790.0,
    "macronutrients": {
      "carbohydrate_g": 201.0,
      "protein_g": 134.0,
      "fat_g": 50.0
    }
  }
}
```

---

### B. Endpoint Pelacak Hidrasi Air Harian

#### `POST /api/v1/user/water-log`
- **Tujuan**: Mencatat penambahan asupan air minum pengguna hari ini (+250ml).
- **Request Body**:
```json
{
  "amount_ml": 250,
  "timestamp": 1764000000000
}
```
- **Response Body (200 OK)**:
```json
{
  "date": "2026-11-24",
  "total_water_ml": 1250,
  "total_glasses": 5,
  "target_glasses": 8
}
```

---

### C. Endpoint Rekomendasi Menu Pintar

#### `GET /api/v1/recommendations/meals?remaining_kcal=860&category=Nusantara`
- **Tujuan**: Mengambil rekomendasi menu makan malam/siang kontekstual berdasarkan sisa kalori & preferensi rasa pengguna.
- **Response Body (200 OK)**:
```json
{
  "remaining_kcal": 860.0,
  "recommendations": [
    {
      "meal_id": "rec_soto_ayam",
      "title": "Soto Ayam Bening + Nasi ½ Porsi",
      "calories": 480,
      "protein_g": 34,
      "carbohydrate_g": 52,
      "fat_g": 12,
      "category": "Nusantara",
      "description": "Kuah bening rendah lemak jenuh dengan irisan dada ayam empuk dan setengah porsi nasi."
    },
    {
      "meal_id": "rec_ayam_bakar",
      "title": "Dada Ayam Bakar + Tumis Kangkung",
      "calories": 430,
      "protein_g": 42,
      "carbohydrate_g": 28,
      "fat_g": 14,
      "category": "Tinggi Protein",
      "description": "Dada ayam tanpa kulit dipanggang rempah Nusantara disajikan dengan kangkung segar."
    }
  ]
}
```
