# PowerTunnel-GTRedirect

Plugin PowerTunnel yang redirect request `server_data.php` dari
`growtopia1.com` / `growtopia2.com` ke server pribadi (KingPS), lewat sebuah
domain (`target_domain`, mis. `lyu.my.id`) yang di-resolve ulang setiap kali
ada request masuk -- jadi kalau IP-nya berubah (mis. playit.gg tunnel
restart), plugin ini otomatis ikut, tanpa perlu update plugin/app.

## Cara kerja

Host `growtopia1.com`/`growtopia2.com` biasanya diminta client lewat HTTP
plain (bukan HTTPS) ke `server_data.php`, dengan response berupa teks:

```
server|<ip>
port|<port>
type|1
```

Plugin ini men-short-circuit request itu (mirip cara plugin AdBlock resmi
mem-block host lewat `request.setResponse(...)`) dan langsung balikin teks
response di atas, dengan `<ip>` hasil resolve `target_domain` saat itu juga,
dan `<port>` dari config `static_port` (konstan -- port publik tunnel game
playit.gg biasanya statis, cuma IP-nya yang berubah).

Traffic ENet (UDP) gameplay-nya sendiri **tidak** disentuh plugin ini --
setelah client dapat IP:port hasil rewrite, dia connect langsung, dan
playit.gg/servermu yang urus sisanya.

**Catatan:** ini menargetkan versi client yang masih pakai HTTP plain untuk
`server_data.php`. Kalau ternyata client target sudah pindah ke HTTPS,
pendekatan short-circuit sederhana ini tidak akan kena -- perlu variant
MITM/SNI-based terpisah. Cek dulu pakai traffic capture (mitmproxy/tcpdump).

## Konfigurasi (lewat UI plugin settings di app)

| Key | Default | Keterangan |
|---|---|---|
| `target_domain` | `lyu.my.id` | Domain yang di-resolve ke IP tunnel terkini |
| `static_port` | `43210` | Port publik tunnel game (playit.gg), konstan |
| `override_domains` | `growtopia1.com,growtopia2.com` | Host yang di-intercept |

## Build

```
./gradlew jar
```

Hasilnya ada di `build/libs/gtredirect-1.0.0.jar` -- file inilah yang
ditambahkan lewat tombol "+" di layar Plugins pada PowerTunnel-Android
(bukan dengan mengedit/commit ke repo APK-nya).

## Lisensi

GPL-3.0, mengikuti lisensi PowerTunnel (krlvm) yang menyediakan SDK-nya.
