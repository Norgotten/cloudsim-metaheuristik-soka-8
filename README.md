<h1 align="center">Penjadwalan Task CloudSim dengan OOA dan ChOA</h1>
<h3 align="center">Strategi Optimasi Komputasi Awan (SOKA) 2026 · Kelompok 8</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Simulator-CloudSim_Plus_7.3.0-0078D4?style=flat-square" alt="CloudSim Plus">
  <img src="https://img.shields.io/badge/Metaheuristik-OOA_%7C_ChOA-E53935?style=flat-square" alt="OOA dan ChOA">
  <img src="https://img.shields.io/badge/Pembanding-OLB-4CAF50?style=flat-square" alt="OLB">
  <img src="https://img.shields.io/badge/Infrastruktur-4_Host_%7C_10_VM-9C27B0?style=flat-square" alt="Infrastruktur">
  <img src="https://img.shields.io/badge/Dataset-GoCJ_1000-FFA000?style=flat-square" alt="GoCJ">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Maven-build-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven">
</p>

---

## Anggota Kelompok 8

| No. | Nama | NRP |
| :---: | :--- | :---: |
| 1 | Dimas Muhammad Putra | 5027241076 |
| 2 | Syifa Nurul Alfiah | 5027241019 |
| 3 | Hafiz Ramadhan | 5027241096 |
| 4 | Muhammad Ahsani Taqwiim Rakhman | 5027241099 |
| 5 | Evan Christian Nainggolan | 5027241026 |

---

## Daftar Isi

1. [Ringkasan](#1-ringkasan)
2. [Daftar istilah](#2-daftar-istilah)
3. [Algoritma](#3-algoritma)
4. [Penerapan metaheuristik ke penjadwalan](#4-penerapan-metaheuristik-ke-penjadwalan)
5. [Konfigurasi simulasi](#5-konfigurasi-simulasi)
6. [Skenario pengujian](#6-skenario-pengujian)
7. [Empat metrik evaluasi](#7-empat-metrik-evaluasi)
8. [Hasil pengujian](#8-hasil-pengujian)
9. [Kesimpulan](#9-kesimpulan)
10. [Keterbatasan](#10-keterbatasan)
11. [Menjalankan program](#11-menjalankan-program)
12. [Struktur project](#12-struktur-project)
13. [Referensi](#13-referensi)

---

## 1. Ringkasan

Di tugas sebelumnya, kami menguji algoritma heuristik **Opportunistic Load Balancing (OLB)** di CloudSim dan di Microsoft Azure. Di repository ini, penjadwalan task di CloudSim dilakukan oleh dua algoritma **metaheuristik**:

1. **Osprey Optimization Algorithm (OOA)**, terinspirasi cara elang tiram berburu ikan.
2. **Chimp Optimization Algorithm (ChOA)**, terinspirasi cara simpanse berburu secara berkelompok.

OLB dipakai sebagai pembanding. Ketiga algoritma diuji pada infrastruktur yang sama (4 host, 10 VM heterogen), dataset yang sama (GoCJ), dan empat metrik yang sama: makespan, total cost, resource utilization, dan degree of imbalance.

Setiap algoritma dijalankan terpisah lewat satu perintah dan menyimpan hasilnya ke CSV, lalu hasil ketiganya dibandingkan.

## 2. Daftar istilah

| Istilah | Arti sederhana |
| :--- | :--- |
| **Task / Cloudlet** | Satu pekerjaan dari dataset GoCJ. Ukurannya dalam **MI** (juta instruksi). Di CloudSim disebut cloudlet. |
| **Host** | Server fisik di dalam datacenter. Tempat VM berjalan. |
| **VM** | Mesin virtual tempat task dikerjakan. Kecepatannya dalam **MIPS** (juta instruksi per detik). |
| **Broker** | "Kurir" di CloudSim yang mengantar task ke VM. Keputusan task ke VM mana dibuat oleh algoritma, broker hanya mengantar. |
| **Heuristik** | Aturan sederhana yang langsung memberi jawaban, misalnya OLB: "kirim ke VM yang paling cepat kosong". |
| **Metaheuristik** | Algoritma yang mencoba banyak kemungkinan jawaban secara bertahap, lalu menyimpan yang terbaik. OOA dan ChOA termasuk di sini. |
| **Agen** | Satu calon jawaban di dalam metaheuristik. Di OOA disebut osprey, di ChOA disebut chimp. Di sini, satu agen = satu rencana pembagian task ke VM. |
| **Populasi** | Kumpulan agen. Di sini berisi 30 agen. |
| **Iterasi** | Satu putaran ketika semua agen bergerak mencari jawaban yang lebih baik. Di sini 1.000 iterasi. |
| **Fitness** | Nilai yang menunjukkan seberapa bagus satu agen. Di sini fitness = estimasi makespan, makin kecil makin baik. |
| **Konvergensi** | Perubahan fitness terbaik dari iterasi ke iterasi. Kurvanya menunjukkan seberapa cepat algoritma menemukan jawaban bagus. |
| **Makespan** | Waktu dari task pertama mulai sampai task **terakhir** selesai. Makin kecil makin baik. |
| **Run** | Satu kali pengujian dengan sejumlah task tertentu. |
| **Seed** | Angka awal pembangkit bilangan acak. Seed yang sama memberi hasil yang sama persis, sehingga pengujian bisa diulang. |

## 3. Algoritma

### 3.1 Osprey Optimization Algorithm (OOA)

OOA meniru osprey yang mencari ikan, menyelam menangkapnya, lalu membawanya ke tempat yang aman untuk dimakan. Setiap iterasi, setiap osprey menjalankan dua fase:

1. **Fase 1, berburu ikan (eksplorasi).** Osprey memilih satu "ikan" secara acak dari osprey lain yang lebih baik darinya (termasuk solusi terbaik), lalu bergerak ke arahnya:
   `x_baru = x + r × (ikan − I × x)`, dengan r acak di [0, 1] dan I acak 1 atau 2.
2. Posisi baru hanya diterima kalau fitness-nya lebih baik.
3. **Fase 2, membawa ikan (eksploitasi).** Osprey bergeser sedikit di sekitar posisinya, dan jarak gesernya makin kecil seiring iterasi:
   `x_baru = x + (lb + r × (ub − lb)) / t`, dengan t = nomor iterasi.
4. Posisi baru hanya diterima kalau fitness-nya lebih baik.
5. Solusi terbaik diperbarui. Langkah 1 sampai 5 diulang sampai iterasi habis.

Setiap osprey dievaluasi **2 kali** per iterasi (sekali per fase).

### 3.2 Chimp Optimization Algorithm (ChOA)

ChOA meniru simpanse yang berburu berkelompok dengan empat peran: **attacker**, **barrier**, **chaser**, dan **driver**. Empat chimp terbaik menjadi pemimpin dengan peran tersebut, dan chimp lain bergerak mengikuti keempatnya.

1. Untuk setiap pemimpin, hitung jarak chimp ke pemimpin: `d = |c × x_pemimpin − m × x|`.
2. Hitung posisi usulan dari setiap pemimpin: `x_k = x_pemimpin − a × d`.
3. Posisi baru = rata-rata keempat usulan: `x_baru = (x1 + x2 + x3 + x4) / 4`.
4. Setelah semua chimp bergerak, keempat pemimpin diperbarui kalau ada chimp yang lebih baik.
5. Langkah 1 sampai 4 diulang sampai iterasi habis.

Keterangan: `f` turun dari 2,5 ke 0 selama iterasi; `a = 2 × f × r1 − f`; `c = 2 × r2`; `m` adalah nilai **chaotic map** (di sini logistic map) yang membuat gerakan simpanse sedikit tak terduga supaya tidak cepat terjebak. Setiap chimp dievaluasi **1 kali** per iterasi.

### 3.3 Opportunistic Load Balancing (OLB, pembanding)

Aturan OLB sama dengan repository CloudSim OLB kelompok 8:

1. Ready time setiap VM diisi 0.
2. Untuk setiap task, pilih VM dengan ready time terkecil.
3. Ready time VM itu ditambah `panjang task / (jumlah core × MIPS)`.

OLB tidak melihat kecepatan VM saat memilih, dan tidak mencoba alternatif lain.

## 4. Penerapan metaheuristik ke penjadwalan

OOA dan ChOA aslinya dibuat untuk mencari titik minimum sebuah fungsi angka. Supaya bisa menjadwalkan task, ada tiga hal yang perlu ditentukan.

### 4.1 Encoding: agen sebagai rencana pembagian task

Posisi satu agen adalah deretan angka desimal **sepanjang jumlah task**, nilainya di rentang [−5, 5]. Angka ke-i menentukan VM untuk task ke-i:

```text
VM untuk task ke-i = floor(posisi[i] + 5)

contoh posisi : [ 3.7, -4.2, 0.1, -0.6, ... ]
VM            : [   8,    0,   5,    4, ... ]
```

Rentang dibuat simetris di sekitar 0 supaya rumus fase 2 OOA bisa menggeser posisi ke dua arah.

### 4.2 Fitness: menilai rencana tanpa menjalankan CloudSim

Selama pencarian, OOA dan ChOA menilai puluhan ribu rencana. Kalau setiap rencana disimulasikan di CloudSim, prosesnya akan sangat lama. Karena itu setiap rencana dinilai dengan **rumus cepat**:

1. Task di setiap VM dikerjakan berurutan sesuai urutan dataset.
2. Setiap task memakai 1 core. VM LARGE punya 2 core, jadi bisa mengerjakan 2 task bersamaan.
3. Waktu satu task = `panjang task / MIPS`.
4. Fitness = waktu selesai core yang paling lama = **estimasi makespan**.

Rencana terbaik baru dijalankan di CloudSim **satu kali** untuk mendapatkan metrik resmi. Kolom `estimasi_makespan` dan `makespan` di CSV hasil bisa dibandingkan untuk menunjukkan bahwa rumus cepat ini akurat.

### 4.3 Inisialisasi hibrida

Dari 30 agen di populasi awal, **1 agen diisi solusi OLB** dan 29 sisanya acak. Alasannya: dengan ratusan sampai 1.000 task, setiap langkah OOA dan ChOA mengubah posisi semua task sekaligus, sehingga populasi yang 100% acak sulit mencapai solusi yang bagus. Pada uji coba awal (1 run per ukuran task), versi 100% acak hanya unggul dari OLB pada 100 task untuk OOA dan 100 sampai 300 task untuk ChOA, lalu kalah jauh di atasnya. Pada 1.000 task, makespan-nya sekitar 21.000 detik, sedangkan OLB 13.974 detik. Versi murni ini tetap bisa dijalankan (lihat [bagian 11](#11-menjalankan-program)).

### 4.4 Alur program

```text
Dataset GoCJ (N baris pertama)
        |
        v
Scheduler (OOA / ChOA / OLB)  -----> menghasilkan: task ke-i -> VM nomor berapa
        |                             (OOA/ChOA: 30 agen x 1.000 iterasi, dinilai dengan fitness)
        v
CloudSim Plus
  Datacenter -> 4 Host -> 10 VM
  Broker mengantar setiap task ke VM sesuai keputusan scheduler
  Simulasi dijalankan
        |
        v
Hitung 4 metrik  ----->  hasil/hasil_<ALG>.csv, konvergensi_<ALG>.csv, vm_<ALG>.csv
```

## 5. Konfigurasi simulasi

Konfigurasi infrastruktur sama dengan repository CloudSim OLB kelompok 8.

### 5.1 Host fisik

Satu datacenter dengan 4 host. Semua host memiliki bandwidth 10.000 Mbps dan storage 1.000.000 MB.

| Host | Grade | Core | MIPS per core | RAM |
| :--- | :--- | :---: | :---: | :---: |
| Host 0, Host 1 | High Performance | 4 | 2.000 | 16 GB |
| Host 2, Host 3 | Standard | 4 | 1.000 | 8 GB |

### 5.2 Mesin virtual

| VM | Tipe | vCPU | MIPS per core | RAM | Tarif per detik |
| :--- | :--- | :---: | :---: | :---: | :---: |
| VM 0 sampai VM 1 | LARGE | 2 | 1.000 | 4 GB | $0,12 |
| VM 2 sampai VM 5 | MEDIUM | 1 | 1.000 | 2 GB | $0,05 |
| VM 6 sampai VM 9 | SMALL | 1 | 500 | 1 GB | $0,02 |

Total kapasitas 10 VM adalah 12 core dan 10.000 MIPS. Setiap VM memakai `CloudletSchedulerSpaceShared`: satu core mengerjakan satu task pada satu waktu, task lain menunggu giliran.

### 5.3 Penempatan VM ke host

Penempatan memakai `GradedVmAllocationPolicy`: VM LARGE dan MEDIUM ke host High Performance, VM SMALL ke host Standard.

| Host | VM yang ditempatkan |
| :--- | :--- |
| Host 0 | VM 0, VM 1 (LARGE) |
| Host 1 | VM 2, VM 3, VM 4, VM 5 (MEDIUM) |
| Host 2 | VM 6, VM 7, VM 8, VM 9 (SMALL) |
| Host 3 | tidak ada VM (cadangan) |

## 6. Skenario pengujian

Pengujian memakai pendekatan **trace-driven**: N task diambil dari N baris pertama `GoCJ_Dataset_1000.csv`, berurutan. Hanya kolom pertama (panjang task dalam MI) yang dipakai.

| Parameter | Nilai |
| :--- | :--- |
| Jumlah task | 100, 200, 300, 400, 500, 600, 700, 800, 900, 1.000 |
| Pengulangan | 3 run per jumlah task, total 30 run per algoritma |
| Algoritma | OOA, ChOA, OLB |
| Populasi OOA dan ChOA | 30 agen |
| Iterasi OOA dan ChOA | 1.000 |
| Seed acak | `1000 × jumlah_task + nomor_run` |

Populasi dan iterasi OOA dan ChOA dibuat sama supaya perbandingannya adil. Karena inputnya trace-driven, OLB memberi hasil yang identik di ketiga run. OOA dan ChOA berbeda antar run karena memakai bilangan acak dengan seed berbeda, sehingga simpangan baku antar run menunjukkan seberapa stabil hasilnya. Seed dibuat tetap, jadi menjalankan ulang program akan memberi angka yang sama persis.

## 7. Empat metrik evaluasi

| Metrik | Rumus | Arti |
| :--- | :--- | :--- |
| **Makespan** (detik) | `waktu selesai task terakhir − waktu mulai task pertama` | Lama seluruh task selesai. Makin kecil makin baik |
| **Total Cost** (USD) | `Σ (waktu eksekusi task × tarif VM yang menjalankannya)` | Biaya pemakaian VM. Makin kecil makin baik |
| **Resource Utilization** (%) | `Σ waktu sibuk semua VM / (10 × makespan) × 100` | Seberapa sibuk VM selama makespan. Makin besar makin baik |
| **Degree of Imbalance** | `(sibuk terlama − sibuk tersingkat) / rata-rata sibuk` | Seberapa timpang beban antar VM. 0 = merata |

**Waktu sibuk** bukan metrik tersendiri, hanya dipakai untuk menghitung Resource Utilization dan Degree of Imbalance:

```text
Waktu sibuk VM j = jumlah waktu eksekusi semua task di VM j / jumlah core VM j
```

Pembagian dengan jumlah core diperlukan karena VM LARGE bisa mengerjakan 2 task bersamaan.

Makespan di sini adalah waktu selesai sebenarnya, mengikuti versi real world. Repository CloudSim OLB yang lama menghitung makespan dari jumlah `actualCpuTime`, sehingga angka OLB di sini jauh lebih kecil daripada di repository lama.

Untuk OOA dan ChOA, dicatat juga **kurva konvergensi** dan **waktu komputasi scheduler** (berapa milidetik algoritma berpikir sebelum simulasi dijalankan).

## 8. Hasil pengujian

Seluruh 90 run (30 per algoritma) selesai tanpa error dan dijalankan ulang di dua komputer berbeda dengan hasil yang identik, karena seed acaknya tetap. Tabel lengkap setiap run, rata-rata, dan simpangan baku keempat metrik ada di [HASIL_PENGUJIAN.md](HASIL_PENGUJIAN.md). Data mentahnya ada di folder `hasil/`.

### 8.1 Ringkasan makespan

Rata-rata 3 run, dalam detik. Angka tebal = metaheuristik yang lebih baik. Kolom "vs OLB" menunjukkan seberapa jauh makespan lebih pendek dibanding OLB.

| Jumlah task | OOA | ChOA | OLB | OOA vs OLB | ChOA vs OLB |
| :---: | :---: | :---: | :---: | :---: | :---: |
| 100 | 1.716,61 | **1.361,41** | 2.727,20 | −37,06 % | −50,08 % |
| 200 | 2.621,70 | **2.462,37** | 2.727,76 | −3,89 % | −9,73 % |
| 300 | 4.543,62 | **4.133,03** | 5.150,64 | −11,79 % | −19,76 % |
| 400 | 5.455,13 | **5.307,87** | 5.520,27 | −1,18 % | −3,85 % |
| 500 | **6.747,30** | 6.812,42 | 6.905,91 | −2,30 % | −1,35 % |
| 600 | 8.299,70 | **8.045,36** | 8.394,53 | −1,13 % | −4,16 % |
| 700 | **9.058,76** | 9.246,23 | 9.715,47 | −6,76 % | −4,83 % |
| 800 | 10.433,76 | **10.330,08** | 10.849,84 | −3,83 % | −4,79 % |
| 900 | **11.758,47** | 11.822,41 | 12.090,53 | −2,75 % | −2,22 % |
| 1000 | 13.541,20 | **13.326,11** | 13.973,89 | −3,10 % | −4,64 % |
| **Rata-rata** | | | | **−7,38 %** | **−10,54 %** |

![Makespan](grafik/01_makespan.png)

![Selisih makespan terhadap OLB](grafik/05_selisih_makespan_vs_olb.png)

### 8.2 Makespan

**OOA dan ChOA selalu menghasilkan makespan yang lebih pendek dari OLB** di kesepuluh ukuran task. Rata-rata, makespan OOA 7,38% lebih pendek dan ChOA 10,54% lebih pendek. ChOA unggul atas OOA di 7 dari 10 ukuran task, OOA unggul di 500, 700, dan 900 task.

Selisih terbesar ada di jumlah task kecil: 37,06% (OOA) dan 50,08% (ChOA) pada 100 task, lalu 11,79% dan 19,76% pada 300 task. Penyebabnya terlihat di data per VM: OLB memilih VM yang paling cepat kosong tanpa melihat panjang task, sehingga task ke-91 (900.000 MI, task terpanjang di dataset) jatuh ke VM 7 yang bertipe SMALL (500 MIPS). Task itu sendiri butuh 1.800 detik, dan VM 7 menjadi yang paling akhir selesai dengan waktu sibuk 2.726,65 detik. Hal yang sama terjadi di 200 task, sehingga makespan OLB di 100 dan 200 task hampir sama (sekitar 2.727 detik). OOA dan ChOA menilai rencana dari makespan keseluruhan, sehingga task panjang cenderung dipindahkan ke VM yang lebih cepat.

Mulai 400 task, selisihnya mengecil menjadi 1 sampai 7%. Ini karena OLB sendiri sudah mendekati batas teoritis. Batas bawah makespan dihitung sebagai yang lebih besar antara `total MI / 10.000 MIPS` (seluruh kapasitas terpakai penuh) dan `task terpanjang / 1.000 MIPS`. Angka ini batas bawah kasar dan belum tentu bisa dicapai.

| Jumlah task | Batas bawah (detik) | OLB | OOA | ChOA |
| :---: | :---: | :---: | :---: | :---: |
| 100 | 1.203 | +126,7 % | +42,7 % | +13,2 % |
| 500 | 6.437 | +7,3 % | +4,8 % | +5,8 % |
| 1000 | 12.966 | +7,8 % | +4,4 % | +2,8 % |

Dengan banyak task, task-task kecil "mengisi celah" sehingga semua VM sibuk hampir sepanjang waktu, termasuk dengan OLB. Ruang untuk perbaikan tinggal beberapa persen.

**Tidak ada satu pun run OOA atau ChOA yang lebih buruk dari OLB.** Ini dijamin oleh inisialisasi hibrida: solusi OLB ada di populasi awal, dan kedua algoritma tidak pernah membuang solusi terbaiknya. Ada 7 dari 60 run yang hasilnya sama persis dengan OLB, artinya algoritma tidak menemukan perbaikan: OOA pada (200 task, run 3), (400, run 3), (600, run 2), dan ChOA pada (500, run 1), (500, run 2), (700, run 3), (900, run 3).

**Kestabilan.** Simpangan baku makespan OOA antar run berkisar 42 sampai 224 detik, ChOA 20 sampai 406 detik. ChOA lebih sering menghasilkan makespan terbaik, tetapi hasilnya lebih bervariasi antar run, terutama di 600 dan 700 task.

### 8.3 Total cost

![Total cost](grafik/02_total_cost.png)

Berbeda dengan makespan, **total cost ketiga algoritma hampir sama**. Mulai 400 task, selisih cost OOA dan ChOA terhadap OLB tidak lebih dari 0,9%. Selisih yang lebih besar hanya muncul di 100 task (OOA −5,13%, ChOA +3,81%), 200 task (ChOA +2,09%), dan 300 task (OOA −2,02%).

Ini karena cost hanya bergantung pada **berapa banyak MI yang dikerjakan di tiap tipe VM**, bukan pada urutan atau waktu selesainya. Karena setiap task memakai 1 core, biaya per 1.000 MI untuk setiap tipe VM adalah:

| Tipe VM | Kecepatan per task | Tarif per detik | Biaya per 1.000 MI |
| :--- | :---: | :---: | :---: |
| SMALL | 500 MIPS | $0,02 | $0,04 |
| MEDIUM | 1.000 MIPS | $0,05 | $0,05 |
| LARGE | 1.000 MIPS | $0,12 | $0,12 |

Untuk mendapat makespan sekecil mungkin, beban harus dibagi sebanding kapasitas VM: LARGE 40%, MEDIUM 40%, SMALL 20% dari total MIPS. Pada 1.000 task, ketiga algoritma memang berakhir di pembagian itu (porsi MI di LARGE/MEDIUM/SMALL: OLB 39,7/39,9/20,4%, OOA 39,5/40,1/20,4%, ChOA 39,2/40,5/20,3%), sehingga cost-nya juga hampir sama.

Pada 100 task, pembagiannya berbeda. OOA hanya menaruh 28,6% MI di VM LARGE yang paling mahal, sehingga cost-nya paling rendah. ChOA menaruh 37,6%, sehingga cost-nya paling tinggi. Fitness OOA dan ChOA hanya memperhitungkan makespan, jadi perbedaan cost ini efek samping, bukan hasil optimasi.

### 8.4 Resource utilization

![Resource utilization](grafik/03_resource_utilization.png)

Resource utilization OOA dan ChOA **lebih tinggi dari OLB di seluruh 10 ukuran task**. Perbedaan terbesar ada di 100 task: OLB 49,44%, OOA 74,83%, ChOA 89,28%. Pada OLB, sebagian besar VM sudah selesai sekitar detik ke-1.100 lalu menganggur, sementara VM 7 masih bekerja sampai detik ke-2.727. Mulai 400 task, ketiga algoritma berada di atas 90%.

Metrik ini bergerak searah dengan makespan. Total waktu sibuk VM hampir tetap untuk jumlah task yang sama, dan yang berubah adalah pembaginya (`10 × makespan`). Makespan yang lebih pendek otomatis membuat utilisasi lebih tinggi.

### 8.5 Degree of imbalance

![Degree of imbalance](grafik/04_degree_of_imbalance.png)

Pada 100 task, OOA (0,79) dan ChOA (0,41) jauh lebih seimbang dibanding OLB (1,26). Mulai 200 task, **hasilnya tidak konsisten**: ketiga algoritma berada di rentang 0,03 sampai 0,48, dan di beberapa ukuran OOA atau ChOA justru sedikit lebih timpang dari OLB, misalnya ChOA di 300 task (0,48 berbanding 0,41) dan 400 task (0,15 berbanding 0,09).

Penyebabnya, fitness hanya melihat VM yang **paling lama** selesai. Degree of imbalance juga memperhitungkan VM yang **paling cepat** selesai. Sebuah rencana bisa memperpendek makespan sambil membiarkan satu VM lain sangat longgar, dan bagi fitness rencana itu tetap lebih baik.

### 8.6 Konvergensi

![Konvergensi](grafik/06_konvergensi.png)

Kurva menunjukkan fitness terbaik (estimasi makespan) di setiap iterasi, rata-rata dari 3 run. Garis putus-putus adalah estimasi makespan OLB. Kedua algoritma berperilaku sangat berbeda:

- **OOA cepat konvergen lalu berhenti membaik.** Hampir seluruh perbaikan terjadi di 100 iterasi pertama. Perbaikan terakhir terjadi antara iterasi 27 dan 288 di semua run yang ditampilkan, setelah itu nilainya datar sampai iterasi 1.000. Fase 2 OOA menggeser posisi sejauh `(lb + r(ub − lb)) / t`, sehingga di iterasi akhir geserannya terlalu kecil untuk memindahkan task ke VM lain.
- **ChOA lambat di awal, lalu membaik tajam di akhir.** Pada 500 dan 1.000 task, ChOA tidak menemukan solusi yang lebih baik dari OLB selama ratusan iterasi pertama. Perbaikan pertama baru muncul di iterasi 673 sampai 841, dan di 2 dari 3 run pada 500 task tidak muncul sama sekali. Ini terjadi karena nilai `f` turun dari 2,5 ke 0. Selama `f` masih besar, chimp bergerak jauh dari pemimpin (eksplorasi), dan hasilnya hampir selalu lebih buruk. Setelah `f` kecil, chimp mencari di sekitar pemimpin (eksploitasi), dan di sinilah perbaikan terjadi. Pada 100 task, ChOA membaik secara bertahap sepanjang iterasi.

Artinya, menambah iterasi kemungkinan besar tidak membantu OOA, tetapi bisa membantu ChOA karena fase eksploitasinya menjadi lebih panjang. Hal ini belum diuji.

### 8.7 Waktu komputasi scheduler

![Waktu scheduler](grafik/07_waktu_scheduler.png)

OLB memutuskan pembagian task dalam kurang dari 1 milidetik. OOA butuh 85 sampai 666 milidetik per run, ChOA 174 sampai 2.349 milidetik. Waktu ini diukur di laptop penguji, jadi angka absolutnya bergantung perangkat, tetapi perbandingannya tetap berlaku.

ChOA sekitar 3 kali lebih lambat dari OOA, padahal OOA melakukan evaluasi fitness 2 kali lebih banyak (60.030 berbanding 30.030 per run). Penyebab yang paling mungkin ada di rumus gerak: untuk setiap task, ChOA menghitung jarak ke 4 pemimpin dan membangkitkan 8 bilangan acak serta 4 nilai chaotic map, sedangkan OOA hanya membangkitkan 3 bilangan acak untuk kedua fasenya. Hal ini belum diukur secara terpisah.

Dibanding waktu yang dihemat, biaya komputasi ini kecil. Pada 1.000 task, ChOA berpikir sekitar 2,3 detik untuk memperpendek makespan sekitar 648 detik.

### 8.8 Akurasi estimasi fitness

Selisih antara estimasi makespan yang dipakai sebagai fitness dan makespan hasil CloudSim rata-rata hanya 0,117%, paling besar 0,299%, di seluruh 90 run. Jadi rumus cepat di [bagian 4.2](#42-fitness-menilai-rencana-tanpa-menjalankan-cloudsim) cukup akurat menggantikan simulasi selama pencarian. Makespan CloudSim selalu sedikit lebih besar dari estimasi, kemungkinan karena CloudSim menambahkan jeda kecil yang tidak dimodelkan, misalnya waktu transfer file task.

## 9. Kesimpulan

1. OOA dan ChOA menghasilkan makespan lebih pendek dari OLB di seluruh ukuran task, rata-rata 7,38% (OOA) dan 10,54% (ChOA). Keunggulannya paling besar di jumlah task kecil, sampai 50,08% pada 100 task, karena OLB bisa menaruh task yang sangat panjang di VM yang lambat.
2. Pada jumlah task besar, selisihnya tinggal beberapa persen karena OLB sudah dekat dengan batas teoritis. Pada 1.000 task, ChOA hanya 2,8% di atas batas bawah, OOA 4,4%, dan OLB 7,8%.
3. ChOA lebih sering memberi makespan terbaik (7 dari 10 ukuran task), tetapi hasilnya lebih bervariasi antar run dan komputasinya sekitar 3 kali lebih lama dari OOA.
4. Total cost ketiga algoritma hampir sama, karena untuk makespan minimum beban akhirnya dibagi sebanding kapasitas VM. Resource utilization metaheuristik selalu lebih tinggi karena mengikuti makespan. Degree of imbalance hanya jelas membaik di 100 task.
5. OOA konvergen dalam sekitar 100 iterasi lalu berhenti membaik, sedangkan ChOA baru membaik di bagian akhir iterasi.

## 10. Keterbatasan

1. **Inisialisasi hibrida.** Satu agen di populasi awal diisi solusi OLB. Tanpa ini, versi murni hanya unggul dari OLB pada 100 task (OOA) dan 100 sampai 300 task (ChOA) di uji coba awal. Karena itu hasil di atas adalah hasil OOA dan ChOA yang **memperbaiki** OLB, bukan yang mencari dari nol.
2. **Fitness hanya makespan.** Cost dan degree of imbalance tidak ikut dioptimasi, sehingga perbaikannya hanya efek samping.
3. **Jumlah evaluasi tidak sama.** Populasi dan iterasi dibuat sama (30 dan 1.000), tetapi OOA mengevaluasi fitness 2 kali per agen per iterasi, sehingga total evaluasinya 2 kali lipat ChOA.
4. **Parameter tidak di-tuning.** Populasi 30 dan iterasi 1.000 dipilih tanpa pengujian sensitivitas. Pola konvergensi ChOA menunjukkan hasilnya mungkin berubah dengan iterasi yang lebih banyak.
5. **3 run per ukuran task.** Cukup untuk melihat pola, tetapi terlalu sedikit untuk uji statistik.
6. **Simulasi, bukan cloud nyata.** Semua task tersedia di awal, tidak ada overhead jaringan, dan kecepatan VM selalu tetap.

## 11. Menjalankan program

### 11.1 Yang perlu diinstal

Cukup **JDK 17 atau lebih baru**. Cek dengan `java -version`, angka versinya harus 17 ke atas.

Maven **tidak perlu diinstal**. Project ini membawa Maven Wrapper (`mvnw.cmd` untuk Windows, `mvnw` untuk Linux/macOS) yang otomatis mengunduh Maven saat pertama kali dijalankan. CloudSim Plus juga diunduh otomatis dari `pom.xml`. Keduanya butuh internet di run pertama saja.

### 11.2 Perintah

Semua perintah dijalankan dari **folder root project** (folder yang berisi `pom.xml`), karena dataset dibaca lewat path relatif.

**Windows (PowerShell atau terminal VS Code):**

```powershell
.\mvnw.cmd compile exec:java "-Dexec.args=OLB"
.\mvnw.cmd compile exec:java "-Dexec.args=OOA"
.\mvnw.cmd compile exec:java "-Dexec.args=CHOA"
```

**Linux / macOS:**

```bash
./mvnw compile exec:java "-Dexec.args=OLB"
```

Di PowerShell, seluruh argumen `-D...` **wajib** dibungkus tanda kutip seperti di atas. Tanpa kutip, PowerShell memotong argumen di tanda titik dan Maven akan error. Kalau Maven sudah terinstal di komputer, `.\mvnw.cmd` boleh diganti `mvn`.

Urutannya bebas dan boleh dijalankan di waktu berbeda. Perkiraan waktu di laptop biasa: OLB beberapa detik, OOA sekitar 1 menit, ChOA sekitar 2 sampai 3 menit. Menjalankan ulang algoritma yang sama akan menimpa file hasilnya.

| Kebutuhan | Perintah (Windows) |
| :--- | :--- |
| Tes cepat (100 task, 1 run) | `.\mvnw.cmd compile exec:java "-Dexec.args=OOA 100 1"` |
| Jumlah task tertentu | `.\mvnw.cmd compile exec:java "-Dexec.args=OOA 100,500,1000 3"` |
| Versi murni tanpa inisialisasi OLB | `.\mvnw.cmd compile exec:java "-Dexec.args=OOA" "-DinitOlb=false"` |
| Ubah iterasi atau populasi | `.\mvnw.cmd compile exec:java "-Dexec.args=OOA" "-Diter=2000" "-Dpop=50"` |

Pengaturan bawaan juga bisa diubah langsung di `Config.java`.

### 11.3 Membuat grafik dan HASIL_PENGUJIAN.md

Setelah ketiga algoritma dijalankan, buat ulang grafik dan tabel ringkasan dari folder `hasil/`:

```bash
pip install pandas matplotlib
python analisis/buat_grafik.py
```

Grafik tersimpan di folder `grafik/`, tabel di `HASIL_PENGUJIAN.md`.

### 11.4 File hasil

Semua hasil masuk ke folder `hasil/`:

| File | Isi |
| :--- | :--- |
| `hasil_<ALG>.csv` | 4 metrik setiap run, estimasi makespan, waktu komputasi scheduler, jumlah evaluasi fitness |
| `konvergensi_<ALG>.csv` | Fitness terbaik di setiap iterasi (OOA dan ChOA saja) |
| `vm_<ALG>.csv` | Jumlah task dan waktu sibuk setiap VM di setiap run |

## 12. Struktur project

```text
.
├── pom.xml
├── README.md
├── HASIL_PENGUJIAN.md                    tabel lengkap hasil 90 run (dibuat otomatis)
├── mvnw, mvnw.cmd, .mvn/                 Maven Wrapper (tidak perlu instal Maven)
├── hasil/                                CSV hasil pengujian (hasil_, konvergensi_, vm_)
├── grafik/                               7 grafik PNG
├── analisis/buat_grafik.py               pembuat grafik dan HASIL_PENGUJIAN.md
└── src/main/
    ├── java/
    │   ├── Main.java                      titik masuk, baca argumen, tulis CSV
    │   ├── Config.java                    semua pengaturan eksperimen
    │   ├── VmSpec.java                    spesifikasi 10 VM
    │   ├── Dataset.java                   baca GoCJ (trace-driven)
    │   ├── Scheduler.java                 bentuk umum semua algoritma
    │   ├── Fitness.java                   encoding dan estimasi makespan
    │   ├── OoaScheduler.java              Osprey Optimization Algorithm
    │   ├── ChoaScheduler.java             Chimp Optimization Algorithm
    │   ├── OlbScheduler.java              Opportunistic Load Balancing
    │   ├── SimulationRunner.java          bangun CloudSim, jalankan, hitung metrik
    │   └── GradedVmAllocationPolicy.java  penempatan VM ke host
    └── resources/dataset/GoCJ_Dataset_1000.csv
```

## 13. Referensi

1. Dehghani, M., & Trojovský, P. (2023). Osprey optimization algorithm: A new bio-inspired metaheuristic algorithm for solving engineering optimization problems. *Frontiers in Mechanical Engineering*, 8, 1126450. https://doi.org/10.3389/fmech.2022.1126450
2. Khishe, M., & Mosavi, M. R. (2020). Chimp optimization algorithm. *Expert Systems with Applications*, 149, 113338. https://doi.org/10.1016/j.eswa.2020.113338
3. Hussain, A., & Aleem, M. (2018). GoCJ: Google Cloud Jobs dataset for distributed and cloud computing infrastructures. *Data*, 3(4), 38. https://doi.org/10.3390/data3040038
4. CloudSim Plus. https://cloudsimplus.org
