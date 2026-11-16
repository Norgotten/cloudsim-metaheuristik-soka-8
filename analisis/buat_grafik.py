"""
Membuat grafik (folder grafik/) dan HASIL_PENGUJIAN.md dari CSV di folder hasil/.

Jalankan dari folder root project:
    pip install pandas matplotlib
    python analisis/buat_grafik.py
"""
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

ROOT = Path(__file__).resolve().parent.parent
HASIL = ROOT / "hasil"
GRAFIK = ROOT / "grafik"
DATASET = ROOT / "src/main/resources/dataset/GoCJ_Dataset_1000.csv"

ALGO = ["OOA", "CHOA", "OLB"]
LABEL = {"OOA": "OOA", "CHOA": "ChOA", "OLB": "OLB"}
WARNA = {"OOA": "#2a78d6", "CHOA": "#eb6834", "OLB": "#1baf7a"}
SURFACE = "#fcfcfb"
TEKS = "#0b0b0b"
TEKS2 = "#52514e"
GRID = "#e4e3df"

plt.rcParams.update({
    "figure.facecolor": SURFACE,
    "axes.facecolor": SURFACE,
    "axes.edgecolor": GRID,
    "axes.labelcolor": TEKS2,
    "axes.titlecolor": TEKS,
    "axes.titlesize": 13,
    "axes.titleweight": "bold",
    "axes.titlelocation": "left",
    "axes.titlepad": 12,
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.grid": True,
    "axes.grid.axis": "y",
    "grid.color": GRID,
    "grid.linewidth": 0.8,
    "xtick.color": TEKS2,
    "ytick.color": TEKS2,
    "legend.frameon": False,
    "font.size": 10,
})


# ---------------------------------------------------------------- format angka
def angka(x, des=2):
    """Format Indonesia: titik ribuan, koma desimal."""
    s = f"{x:,.{des}f}"
    return s.replace(",", "_").replace(".", ",").replace("_", ".")


def persen(x, des=2):
    return angka(x, des) + " %"


# ---------------------------------------------------------------- data
def muat():
    hasil = pd.concat([pd.read_csv(HASIL / f"hasil_{a}.csv") for a in ALGO])
    konv = pd.concat([pd.read_csv(HASIL / f"konvergensi_{a}.csv") for a in ["OOA", "CHOA"]])
    vm = pd.concat([pd.read_csv(HASIL / f"vm_{a}.csv") for a in ALGO])
    return hasil, konv, vm


def rata(hasil, kolom):
    return hasil.groupby(["jumlah_task", "algoritma"])[kolom].mean().unstack("algoritma")


def simpang(hasil, kolom):
    return hasil.groupby(["jumlah_task", "algoritma"])[kolom].std().unstack("algoritma")


# ---------------------------------------------------------------- grafik
def label_ujung(ax, df, jarak_pt=12):
    """Label nama algoritma di ujung kanan garis, digeser supaya tidak bertumpuk."""
    fig = ax.figure
    fig.canvas.draw()
    px_per_pt = fig.dpi / 72
    x = df.index[-1]
    ujung = []
    for a in df.columns:
        y = df[a].iloc[-1]
        ujung.append([a, y, ax.transData.transform((x, y))[1]])
    ujung.sort(key=lambda u: u[2])
    posisi = [u[2] for u in ujung]
    jarak = jarak_pt * px_per_pt
    for i in range(1, len(posisi)):
        posisi[i] = max(posisi[i], posisi[i - 1] + jarak)
    geser = (sum(u[2] for u in ujung) - sum(posisi)) / len(posisi)
    posisi = [p + geser for p in posisi]
    for i in range(1, len(posisi)):
        posisi[i] = max(posisi[i], posisi[i - 1] + jarak)
    for (a, y, py), target in zip(ujung, posisi):
        ax.annotate(LABEL[a], (x, y), xytext=(7, (target - py) / px_per_pt),
                    textcoords="offset points", color=TEKS, fontsize=9, va="center")


def grafik_garis(df, judul, ylabel, nama, fmt_y=None, legenda="upper left"):
    fig, ax = plt.subplots(figsize=(8, 4.6))
    for a in ALGO:
        ax.plot(df.index, df[a], color=WARNA[a], linewidth=2, marker="o",
                markersize=5.5, markeredgecolor=SURFACE, markeredgewidth=1.5,
                label=LABEL[a])
    ax.set_title(judul)
    ax.set_xlabel("Jumlah task")
    ax.set_ylabel(ylabel)
    ax.set_xticks(df.index)
    ax.set_xlim(df.index[0] - 30, df.index[-1] + 90)
    if fmt_y:
        ax.yaxis.set_major_formatter(fmt_y)
    ax.legend(loc=legenda, ncols=3)
    fig.tight_layout()
    label_ujung(ax, df[ALGO])
    fig.savefig(GRAFIK / nama, dpi=150)
    plt.close(fig)


def fmt_ribuan(des=0):
    return matplotlib.ticker.FuncFormatter(lambda v, _: angka(v, des))


def grafik_selisih(mk):
    sel = pd.DataFrame({a: (mk["OLB"] - mk[a]) / mk["OLB"] * 100 for a in ["OOA", "CHOA"]})
    fig, ax = plt.subplots(figsize=(8, 4.6))
    lebar = 32
    for i, a in enumerate(["OOA", "CHOA"]):
        xs = sel.index + (i - 0.5) * (lebar + 2)
        bars = ax.bar(xs, sel[a], width=lebar, color=WARNA[a], label=LABEL[a])
        for b, v in zip(bars, sel[a]):
            if v >= 10:
                ax.annotate(angka(v, 1) + "%", (b.get_x() + b.get_width() / 2, v),
                            xytext=(0, 3), textcoords="offset points", ha="center",
                            fontsize=8, color=TEKS2)
    ax.axhline(0, color=TEKS2, linewidth=0.8)
    ax.set_title("Makespan lebih pendek dibanding OLB")
    ax.set_xlabel("Jumlah task")
    ax.set_ylabel("Selisih terhadap OLB (%)")
    ax.set_xticks(sel.index)
    ax.legend(loc="upper right")
    fig.tight_layout()
    fig.savefig(GRAFIK / "05_selisih_makespan_vs_olb.png", dpi=150)
    plt.close(fig)
    return sel


def grafik_konvergensi(konv, hasil):
    olb = hasil[hasil.algoritma == "OLB"].groupby("jumlah_task").estimasi_makespan.mean()
    ukuran = [100, 500, 1000]
    fig, axes = plt.subplots(1, 3, figsize=(12, 4))
    for ax, n in zip(axes, ukuran):
        for a in ["OOA", "CHOA"]:
            s = konv[(konv.jumlah_task == n) & (konv.algoritma == a)].groupby("iterasi").best_fitness.mean()
            ax.plot(s.index, s.values, color=WARNA[a], linewidth=2, label=LABEL[a])
        ax.axhline(olb[n], color=WARNA["OLB"], linewidth=1.5, linestyle=(0, (4, 3)), label="OLB")
        ax.set_title(f"{n} task")
        ax.set_xlabel("Iterasi")
        ax.yaxis.set_major_formatter(fmt_ribuan())
    axes[0].set_ylabel("Fitness terbaik (estimasi makespan, detik)")
    axes[0].legend(loc="upper right")
    fig.suptitle("Kurva konvergensi (rata-rata 3 run)", x=0.01, ha="left",
                 fontsize=13, fontweight="bold", color=TEKS)
    fig.tight_layout()
    fig.savefig(GRAFIK / "06_konvergensi.png", dpi=150)
    plt.close(fig)


def grafik_waktu(hasil):
    w = rata(hasil, "waktu_scheduler_ms")
    fig, ax = plt.subplots(figsize=(8, 4.6))
    for a in ALGO:
        ax.plot(w.index, w[a], color=WARNA[a], linewidth=2, marker="o", markersize=5.5,
                markeredgecolor=SURFACE, markeredgewidth=1.5, label=LABEL[a])
    ax.set_title("Waktu komputasi scheduler per run")
    ax.set_xlabel("Jumlah task")
    ax.set_ylabel("Milidetik")
    ax.set_xticks(w.index)
    ax.set_xlim(w.index[0] - 30, w.index[-1] + 90)
    ax.yaxis.set_major_formatter(fmt_ribuan())
    ax.legend(loc="upper left", ncols=3)
    fig.tight_layout()
    label_ujung(ax, w[ALGO])
    fig.savefig(GRAFIK / "07_waktu_scheduler.png", dpi=150)
    plt.close(fig)


# ---------------------------------------------------------------- markdown
def tabel_metrik(hasil, kolom, des, satuan=""):
    m, s = rata(hasil, kolom), simpang(hasil, kolom)
    baris = ["| Jumlah task | OOA | ChOA | OLB |", "| :---: | :---: | :---: | :---: |"]
    for n in m.index:
        sel = []
        for a in ALGO:
            teks = angka(m.loc[n, a], des) + satuan
            if a != "OLB":
                teks += f" ± {angka(s.loc[n, a], des)}"
            sel.append(teks)
        baris.append(f"| {n} | " + " | ".join(sel) + " |")
    return "\n".join(baris)


def tabel_detail(hasil, a):
    d = hasil[hasil.algoritma == a].sort_values(["jumlah_task", "run"])
    baris = ["| Jumlah task | Run | Makespan (detik) | Total cost ($) | Resource utilization | "
             "Degree of imbalance | Estimasi makespan | Waktu scheduler (ms) |",
             "| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |"]
    for _, r in d.iterrows():
        baris.append(f"| {r.jumlah_task} | {r.run} | {angka(r.makespan)} | {angka(r.total_cost)} | "
                     f"{persen(r.resource_utilization)} | {angka(r.degree_of_imbalance, 4)} | "
                     f"{angka(r.estimasi_makespan)} | {angka(r.waktu_scheduler_ms, 0)} |")
    return "\n".join(baris)


def tulis_md(hasil, sel):
    w = rata(hasil, "waktu_scheduler_ms")
    selisih = ["| Jumlah task | OOA | ChOA |", "| :---: | :---: | :---: |"]
    for n in sel.index:
        selisih.append(f"| {n} | {persen(sel.loc[n, 'OOA'])} | {persen(sel.loc[n, 'CHOA'])} |")
    selisih.append(f"| **Rata-rata** | **{persen(sel['OOA'].mean())}** | **{persen(sel['CHOA'].mean())}** |")

    waktu = ["| Jumlah task | OOA | ChOA | OLB |", "| :---: | :---: | :---: | :---: |"]
    for n in w.index:
        waktu.append(f"| {n} | {angka(w.loc[n, 'OOA'], 0)} | {angka(w.loc[n, 'CHOA'], 0)} | "
                     f"{angka(w.loc[n, 'OLB'], 2)} |")

    est = (hasil.makespan - hasil.estimasi_makespan).abs() / hasil.makespan * 100

    teks = f"""# Hasil Pengujian

Berkas ini dibuat otomatis oleh `analisis/buat_grafik.py` dari CSV di folder `hasil/`.
Pembahasan ada di [README.md](README.md#8-hasil-pengujian).

Skenario: trace-driven, 100 sampai 1.000 task, 3 run per jumlah task, 30 run per algoritma.
Nilai OOA dan ChOA ditulis sebagai rata-rata ± simpangan baku dari 3 run. OLB tidak diberi
simpangan baku karena ketiga run-nya identik.

## 1. Rata-rata 3 run

### 1.1 Makespan (detik)

{tabel_metrik(hasil, "makespan", 2)}

### 1.2 Total cost ($)

{tabel_metrik(hasil, "total_cost", 2)}

### 1.3 Resource utilization (%)

{tabel_metrik(hasil, "resource_utilization", 2)}

### 1.4 Degree of imbalance

{tabel_metrik(hasil, "degree_of_imbalance", 4)}

## 2. Makespan lebih pendek dibanding OLB

Dihitung dari rata-rata 3 run: `(makespan OLB − makespan algoritma) / makespan OLB × 100%`.

{chr(10).join(selisih)}

## 3. Waktu komputasi scheduler (milidetik, rata-rata 3 run)

Waktu yang dipakai algoritma untuk memutuskan pembagian task, sebelum simulasi CloudSim dijalankan.

{chr(10).join(waktu)}

## 4. Akurasi estimasi makespan

Selisih antara estimasi makespan (fitness) dan makespan hasil CloudSim pada seluruh 90 run:
rata-rata {persen(est.mean(), 3)}, terbesar {persen(est.max(), 3)}.

## 5. Grafik

![Makespan](grafik/01_makespan.png)

![Total cost](grafik/02_total_cost.png)

![Resource utilization](grafik/03_resource_utilization.png)

![Degree of imbalance](grafik/04_degree_of_imbalance.png)

![Selisih makespan terhadap OLB](grafik/05_selisih_makespan_vs_olb.png)

![Konvergensi](grafik/06_konvergensi.png)

![Waktu scheduler](grafik/07_waktu_scheduler.png)

## 6. Data lengkap per run

### 6.1 OOA

{tabel_detail(hasil, "OOA")}

### 6.2 ChOA

{tabel_detail(hasil, "CHOA")}

### 6.3 OLB

{tabel_detail(hasil, "OLB")}
"""
    (ROOT / "HASIL_PENGUJIAN.md").write_text(teks, encoding="utf-8")


def main():
    GRAFIK.mkdir(exist_ok=True)
    hasil, konv, vm = muat()

    mk = rata(hasil, "makespan")
    grafik_garis(mk, "Makespan (rata-rata 3 run)", "Detik", "01_makespan.png",
                 fmt_ribuan())
    grafik_garis(rata(hasil, "total_cost"), "Total cost (rata-rata 3 run)", "USD",
                 "02_total_cost.png", fmt_ribuan())
    grafik_garis(rata(hasil, "resource_utilization"), "Resource utilization (rata-rata 3 run)",
                 "Persen", "03_resource_utilization.png")
    grafik_garis(rata(hasil, "degree_of_imbalance"), "Degree of imbalance (rata-rata 3 run)",
                 "Degree of imbalance (0 = merata)", "04_degree_of_imbalance.png",
                 legenda="upper right")
    sel = grafik_selisih(mk)
    grafik_konvergensi(konv, hasil)
    grafik_waktu(hasil)
    tulis_md(hasil, sel)
    print("Grafik tersimpan di grafik/, ringkasan di HASIL_PENGUJIAN.md")


if __name__ == "__main__":
    main()
