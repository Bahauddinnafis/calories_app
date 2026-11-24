# Kebutuhan Backend: Menghilangkan Data Dummy (Calories App)

Dokumen ini berisi semua yang dibutuhkan dari tim backend agar seluruh alur aplikasi (onboarding → beranda → scan → koreksi → riwayat → target) memakai data asli. Hasil audit kode Android per 24-11-2026.

Konvensi untuk semua endpoint baru:
- Base path `api/v1`, JSON `snake_case`, angka gizi bertipe number (bukan string), berat dalam gram, energi dalam kkal.
- Timestamp memakai epoch milidetik (UTC). Tanggal memakai `YYYY-MM-DD` sesuai zona waktu yang dikirim klien (header `X-Timezone: Asia/Jakarta`).
- Format error seragam:
  ```json
  { "error": { "code": "product_not_found", "message": "Produk tidak ditemukan", "retryable": false } }
  ```
  `message` harus bisa langsung ditampilkan ke pengguna (bahasa Indonesia, tanpa istilah teknis).
- Teks yang tampil di UI (headline, message, deskripsi menu) dikirim dalam bahasa Indonesia.

---

## Prioritas

| # | Kebutuhan | Status saat ini | Prioritas |
|---|---|---|---|
| 1 | Endpoint rekomendasi menu | 4 menu hardcoded di aplikasi | **P0** |
| 2 | Skema response pencarian produk kemasan & barcode | Endpoint sudah dipanggil, tapi skemanya belum didokumentasikan | **P0** |
| 3 | Field scan: `overall_confidence` asli, `ai_notice` | Aplikasi memalsukan akurasi (dipaksa 80–98,5%) dan memakai catatan gizi statis | **P0** |
| 4 | Saran "kalori tersembunyi" per hidangan + dukungan di recalculate | 3 add-on hardcoded (Kuah Gulai/Kerupuk/Minyak) dan perhitungan lokal | **P1** |
| 5 | Kontrak daily insight yang final | Sudah dipanggil; ada teks fallback lokal | **P1** |
| 6 | Identitas pengguna (auth) | Belum ada sama sekali | **P1** (wajib sebelum #7–#9) |
| 7 | Profil & target harian | Disimpan & dihitung di perangkat | P2 |
| 8 | Sinkronisasi riwayat makan | Hanya di Room (lokal) | P2 |
| 9 | Log air minum | Hanya di perangkat | P2 |
| 10 | Data referensi (preferensi diet, alergi, kategori) | Hardcoded di layar onboarding | P3 |

---

## 1. Rekomendasi Menu (P0)

`GET /api/v1/recommendations/meals`

Query:

| Param | Tipe | Wajib | Keterangan |
|---|---|---|---|
| `remaining_kcal` | number | ya | Sisa kalori hari ini |
| `remaining_protein_g` | number | ya | |
| `remaining_carbs_g` | number | ya | |
| `remaining_fat_g` | number | ya | |
| `meal_type` | string | ya | `breakfast` / `lunch` / `dinner` / `snack` |
| `goal` | string | ya | `lose` / `maintain` / `gain` |
| `preferences` | string (dipisah koma) | tidak | contoh: `Masakan Nusantara,Tinggi Protein,Halal` |
| `allergies` | string (dipisah koma) | tidak | Menu yang mengandung alergen **wajib dikeluarkan** |
| `limit` | int | tidak | default 4 |

Response 200:
```json
{
  "remaining_kcal": 860.0,
  "category": "Tinggi Protein",
  "recommendations": [
    {
      "meal_id": "rec_soto_ayam_bening",
      "title": "Soto Ayam Bening + Nasi ½ Porsi",
      "description": "Kuah bening rendah lemak dengan dada ayam dan setengah porsi nasi.",
      "category": "Nusantara",
      "serving_weight_g": 450.0,
      "nutrition": {
        "energy_kcal": 480.0,
        "protein_g": 34.0,
        "carbohydrate_g": 55.0,
        "fat_g": 12.0
      },
      "reason": "Tinggi protein dan masih muat di sisa kalori makan malammu.",
      "source": { "source_name": "TKPI", "source_version": "2023" },
      "image_url": null
    }
  ]
}
```

Aturan:
- Hanya kembalikan menu dengan `energy_kcal <= remaining_kcal` (toleransi maksimal +10%). Jika tidak ada yang muat, kembalikan menu ringan (snack) dan beri `reason` yang sesuai.
- Urutkan berdasarkan kecocokan dengan makro yang paling kurang (misalnya protein masih jauh dari target → menu tinggi protein di atas).
- Nilai gizi per **1 porsi yang ditampilkan**, karena aplikasi mencatat 1 porsi penuh ke riwayat. `carbohydrate_g`, `fat_g`, dan `serving_weight_g` **wajib** ada.
- `meal_id` harus stabil (dipakai sebagai ID item di daftar).
- `image_url` opsional; jika diisi, foto menu akan ditampilkan di halaman detail.

---

## 2. Produk Kemasan & Barcode (P0)

Endpoint sudah dipanggil aplikasi:
- `GET /api/v1/products/search?query={teks}`
- `GET /api/v1/products/barcode/{barcode}` (barcode 8–14 digit: EAN-8, UPC-A, EAN-13, GTIN-14)

Mohon gunakan skema berikut. Saat ini aplikasi menebak beberapa variasi nama field.

`search` → 200:
```json
{
  "query": "susu uht cokelat",
  "items": [
    {
      "product_id": "prd_8992761111111",
      "barcode": "8992761111111",
      "name": "Susu UHT Cokelat 200 ml",
      "brand": "Ultra Milk",
      "serving_size_g": 200.0,
      "serving_label": "1 kotak (200 ml)",
      "nutrition_per_serving": {
        "energy_kcal": 150.0,
        "protein_g": 6.0,
        "carbohydrate_g": 22.0,
        "fat_g": 4.5,
        "sugar_g": 19.0,
        "sodium_mg": 120.0
      },
      "source": { "source_name": "BPOM / Informasi Nilai Gizi kemasan", "source_version": "2026-10" },
      "image_url": "https://…"
    }
  ]
}
```

`barcode` → 200: satu objek produk dengan struktur yang sama seperti item di atas (tanpa wrapper `items`).
`barcode` → 404 jika barcode tidak dikenal, dengan `error.code = "product_not_found"`.

Aturan:
- `search` minimal 2 karakter, maksimal 20 hasil, diurutkan berdasarkan relevansi. Hasil kosong → `200` dengan `items: []` (bukan 404).
- Nilai gizi **per sajian** sesuai label kemasan (bukan per 100 g), karena aplikasi mencatat 1 sajian.
- Butuh sumber data produk Indonesia (misalnya basis data internal, BPOM, atau Open Food Facts), dan **tim backend perlu memutuskan sumbernya**.

---

## 3. Hasil Scan (P0): `POST /api/v1/scans` dan `/recalculate`

Field berikut sudah ada di DTO aplikasi. Mohon dipastikan **selalu diisi dengan nilai asli**:

| Field | Masalah saat ini | Yang dibutuhkan |
|---|---|---|
| `overall_confidence` | Aplikasi memaksa tampilan ke rentang 80–98,5% | Nilai asli 0.0–1.0. Aplikasi akan menampilkan nilai apa adanya. |
| `ai_notice` + `display_ai_notice` | Kartu "Catatan Gizi" memakai kalimat statis ("protein … sangat mendukung target harian Anda") | Kalimat catatan gizi yang relevan untuk hidangan tersebut. |
| `single_serving_weight_g` | Jika kosong, aplikasi memakai rumus tebakan `berat_total / 5` (80–150 g) | Berat 1 porsi/unit (misalnya 1 potong ayam, 1 tusuk sate) untuk foto berisi banyak unit. Tambahkan juga `unit_name` (misalnya "potong", "tusuk"). |
| `ingredients[].source` / `nutrition_source_type` | Jika kosong, UI menampilkan "Estimasi AI" | Isi sumber gizi (TKPI / USDA / estimasi) per bahan. |

Agar catatan gizi bisa dipersonalisasi, aplikasi dapat mengirim konteks harian bersama upload. Mohon terima field multipart opsional berikut:
`remaining_kcal`, `remaining_protein_g`, `goal`.

---

## 4. Kalori Tersembunyi & Rekalkulasi (P1)

Saat ini layar koreksi menampilkan 3 add-on tetap (Kuah Gulai +90, Kerupuk Kulit +75, Minyak Goreng +110). Totalnya dihitung secara lokal dengan rumus kkal/gram, dan ada bahan cadangan palsu ("Nasi Putih 150 g, Ayam Pop 120 g, Daun Singkong 80 g") ketika hasil scan tidak memiliki bahan.

Yang dibutuhkan:

a. Tambahkan field `suggested_add_ons` pada response `POST /api/v1/scans`, berisi tambahan yang umum untuk hidangan yang terdeteksi:
```json
"suggested_add_ons": [
  { "add_on_id": "kuah_gulai", "name": "Kuah Gulai", "default_weight_g": 50.0, "energy_kcal": 90.0 },
  { "add_on_id": "kerupuk_kulit", "name": "Kerupuk Kulit", "default_weight_g": 15.0, "energy_kcal": 75.0 }
]
```

b. Request `POST /api/v1/scans/{scan_id}/recalculate` menerima add-on:
```json
{
  "meal_name": "Nasi Padang Ayam Pop",
  "ingredients": [ { "food_name": "Nasi Putih", "estimated_weight_g": 150.0 } ],
  "add_ons": [ { "add_on_id": "kuah_gulai", "weight_g": 50.0 } ]
}
```
Response memakai struktur yang sama dengan hasil scan. Total kalori final dihitung oleh backend, bukan oleh aplikasi.

c. (Opsional, tapi sangat membantu) Endpoint pratinjau ringan agar angka estimasi di layar koreksi berasal dari backend dan bukan dari rumus lokal:
`POST /api/v1/nutrition/estimate` dengan body yang sama seperti recalculate, tanpa menyimpan apa pun.

---

## 5. Daily Insight (P1): `GET /api/v1/insights/daily`

Endpoint sudah dipanggil. Yang perlu dipastikan:
- Semua field response terisi: `headline`, `message`, `priority_focus`, `hydration_nudge`, `suggested_category`, `status`.
- Nilai `suggested_category` **harus berasal dari daftar kategori yang sama** dengan field `category` di rekomendasi menu (#1).
- `priority_focus` pendek (maksimal ±20 karakter, misalnya "Protein"), karena ditampilkan sebagai chip.
- Teks fallback di aplikasi hanya akan dipakai ketika perangkat sedang offline.

---

## 6. Identitas Pengguna / Auth (P1)

Aplikasi belum memiliki login atau identitas pengguna, sehingga data per pengguna (#7–#9) belum bisa disimpan di server. Tim backend perlu memutuskan salah satu:
- **Opsi A:** akun anonim. `POST /api/v1/auth/anonymous` mengembalikan `{ "user_id", "access_token", "refresh_token" }`, dan dapat ditautkan ke akun Google nanti.
- **Opsi B:** login penuh (Google Sign-In / Firebase Auth), dengan backend memverifikasi ID token.

Semua endpoint #7–#9 memakai `Authorization: Bearer <token>`.

---

## 7. Profil & Target Harian (P2)

Saat ini profil disimpan di perangkat dan target dihitung di aplikasi (Mifflin-St Jeor, dengan pembagian makro tetap 45/30/25%). Hasil hitungannya asli, tetapi tidak tersinkron dan aturannya tidak bisa diubah dari server.

- `PUT /api/v1/me/profile`: body seperti `POST /user/profile/onboarding` di `backend_api_contract_report.md`, ditambah `display_name`.
- `GET /api/v1/me/profile` mengembalikan profil beserta `calculated_targets`:
  ```json
  "calculated_targets": {
    "bmr_kcal": 1664.0, "tdee_kcal": 2288.0, "daily_energy_kcal": 1790.0,
    "macronutrients": { "carbohydrate_g": 201.0, "protein_g": 134.0, "fat_g": 50.0 },
    "macro_split_pct": { "carbohydrate": 45, "protein": 30, "fat": 25 },
    "water_target_glasses": 8
  }
  ```
- Mohon kirim `macro_split_pct` dan `water_target_glasses` (karena keduanya ditampilkan di UI), agar keduanya tidak lagi hardcoded di aplikasi.

---

## 8. Sinkronisasi Riwayat Makan (P2)

Riwayat saat ini hanya ada di database lokal. Datanya asli, tetapi hilang jika aplikasi di-uninstall atau pengguna berganti perangkat.

- `POST /api/v1/me/meal-logs`: mencatat entri. `source` berisi `scan` / `recommendation` / `product` / `manual`, beserta `scan_id`, `meal_id`, atau `product_id` sesuai sumbernya.
  ```json
  {
    "client_log_id": "rec_rec_soto_ayam_1764000000000",
    "source": "recommendation",
    "reference_id": "rec_soto_ayam_bening",
    "meal_name": "Soto Ayam Bening + Nasi ½ Porsi",
    "meal_type": "dinner",
    "consumed_weight_g": 450.0,
    "nutrition": { "energy_kcal": 480.0, "protein_g": 34.0, "carbohydrate_g": 55.0, "fat_g": 12.0 },
    "logged_at": 1764000000000
  }
  ```
  `client_log_id` dipakai sebagai kunci idempotensi, supaya entri yang dikirim ulang tidak tercatat dua kali.
- `PATCH /api/v1/me/meal-logs/{id}`: mengubah porsi atau jenis makan.
- `DELETE /api/v1/me/meal-logs/{id}`
- `GET /api/v1/me/meal-logs?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/me/summary/weekly`: rata-rata kalori dan konsistensi 7 hari (untuk kartu "Konsistensi 7 Hari Terakhir").
- Foto scan: sertakan `image_url` pada log bertipe `scan` jika foto disimpan di server.

---

## 9. Log Air Minum (P2)

- `POST /api/v1/me/water-logs` dengan body `{ "amount_ml": 250, "logged_at": 1764000000000 }`
- `GET /api/v1/me/water-logs/today` mengembalikan `{ "date", "total_ml", "total_glasses", "target_glasses" }`

---

## 10. Data Referensi (P3)

`GET /api/v1/reference/options` mengembalikan daftar pilihan yang saat ini hardcoded di onboarding:
```json
{
  "dietary_preferences": [ { "key": "nusantara", "label": "Masakan Nusantara" } ],
  "allergies": [ { "key": "seafood", "label": "Makanan Laut (Seafood)" } ],
  "meal_categories": [ { "key": "high_protein", "label": "Tinggi Protein" } ]
}
```
Key ini dipakai oleh #1 (`preferences`, `allergies`) dan #5 (`suggested_category`).

---

## Yang Akan Diperbaiki di Sisi Aplikasi (bukan tugas backend)

Untuk referensi tim backend, berikut data dummy yang akan dihapus di aplikasi:
- Jumlah air minum awal yang diset 5 gelas untuk demo. Akan diubah menjadi 0.
- Nama default "Andi Pratama" dan nilai profil default (25 tahun / 170 cm / 72 kg) ketika profil kosong.
- Persentase akurasi yang dipaksa ke rentang 80–98,5%, serta kalimat "Catatan Gizi" yang statis.
- Bahan cadangan palsu dan nilai 640 kkal di layar koreksi.
- Badge teknis "Room SQLite" di layar Riwayat.
