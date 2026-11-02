#!/usr/bin/env node
/**
 * Importe le Google Sheet des restos vers src/data/restaurants.json.
 *   node scripts/import-restaurants.mjs <url-ou-fichier.csv> [--commune dely-brahim]
 * L'URL du Sheet : https://docs.google.com/spreadsheets/d/<ID>/export?format=csv
 */
import fs from 'node:fs'
import path from 'node:path'

const CUISINES = new Set(['burger','pizza','tacos','sandwich','fast-food','crispy-chicken','pasta','grillades','traditionnel','italien','asiatique','sushi','libanais','poisson','healthy','brunch','cafe','dessert','autre'])
const COMMUNES = new Set(['hydra','ben-aknoun','el-biar','dely-brahim','cheraga','bab-ezzouar','kouba','draria','ouled-fayet','ain-benian'])
const BUDGETS = ['B1','B2','B3','B4','B5']
const FIXES = { 'crispy-chiken': 'crispy-chicken', sandawich: 'sandwich' }

const [src, ...rest] = process.argv.slice(2)
if (!src) { console.error('Usage: import-restaurants.mjs <url|csv> [--commune id]'); process.exit(1) }
const only = rest.includes('--commune') ? rest[rest.indexOf('--commune') + 1] : null

function parseCsv(text) {
  const rows = []; let row = [], cell = '', q = false
  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    if (q) { if (c === '"') { if (text[i + 1] === '"') { cell += '"'; i++ } else q = false } else cell += c }
    else if (c === '"') q = true
    else if (c === ',') { row.push(cell); cell = '' }
    else if (c === '\n' || c === '\r') { if (c === '\r' && text[i + 1] === '\n') i++; row.push(cell); rows.push(row); row = []; cell = '' }
    else cell += c
  }
  if (cell || row.length) { row.push(cell); rows.push(row) }
  return rows.filter((r) => r.some((x) => x.trim()))
}

const slug = (s) => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
const list = (s) => (s ?? '').split(/[;,]/).map((x) => x.trim().toLowerCase()).filter(Boolean)
const orNull = (s) => (s && s.trim() ? s.trim() : null)
/** Résout un lien Google Maps (court ou long) et en extrait les coordonnées du lieu, sans API. */
async function coordsFromMapsUrl(url) {
  try {
    const res = await fetch(url, { redirect: 'follow', headers: { 'user-agent': 'Mozilla/5.0' } })
    const finalUrl = decodeURIComponent(res.url)
    const place = finalUrl.match(/!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)/) // position exacte du lieu
    const view = finalUrl.match(/@(-?\d+\.\d+),(-?\d+\.\d+)/) // centre de la vue (repli)
    const m = place ?? view
    return m ? { lat: Number(m[1]), lng: Number(m[2]) } : null
  } catch {
    return null
  }
}

/** "36.7538887, 2.9949244" (copié depuis Google Maps) → { lat, lng } ; null si illisible. */
function parseCoordinates(text) {
  if (!text) return null
  const nums = text.match(/-?\d+\.\d+/g)
  if (!nums || nums.length !== 2) return null
  return { lat: Number(nums[0]), lng: Number(nums[1]) }
}

const warnings = []
const warn = (id, msg) => warnings.push(`${id}: ${msg}`)

const text = /^https?:/.test(src) ? await (await fetch(src)).text() : fs.readFileSync(src, 'utf8')
const [header, ...lines] = parseCsv(text)
// Noms de colonnes tolérants : espaces, accents et casse ignorés (ex. « Coordonnées »)
const cols = header.map((h) => slug(h).replace(/-/g, '_'))
const out = []
const seen = new Set()

for (const line of lines) {
  const r = Object.fromEntries(cols.map((c, i) => [c, (line[i] ?? '').trim()]))
  const id = r.id
  if (!id) continue
  if (seen.has(id)) { warn(id, 'id en double, ignoré'); continue }
  seen.add(id)
  const commune = slug(r.commune)
  if (!COMMUNES.has(commune)) { warn(id, `commune inconnue "${r.commune}", ignoré`); continue }
  if (only && commune !== only) continue

  const cuisines = list(r.cuisines).map((c) => FIXES[c] ?? c).filter((c) => CUISINES.has(c) || (warn(id, `cuisine inconnue "${c}"`), false))
  if (!cuisines.length) warn(id, 'aucune cuisine valide')

  let prices = list(r.price_range).map((p) => p.toUpperCase()).filter((p) => BUDGETS.includes(p))
  if (!prices.length) warn(id, 'price_range manquant ou invalide')
  else {
    const idx = prices.map((p) => BUDGETS.indexOf(p))
    const lo = Math.min(...idx), hi = Math.max(...idx)
    const full = BUDGETS.slice(lo, hi + 1)
    if (full.length !== new Set(prices).size) warn(id, `paliers non contigus ${prices.join(',')} → complétés en ${full.join(',')}`)
    prices = full
  }

  const insta = orNull(r.instagram)
  const handle = insta ? (insta.match(/instagram\.com\/([^/?#]+)/)?.[1] ?? insta.replace(/^@/, '')) : null
  // Priorité : colonne « coordonnees » (lat, lng) > colonnes lat/lng > lien Google Maps
  const fromColumn = parseCoordinates(r.coordonnees ?? r.coordinates)
  if ((r.coordonnees ?? r.coordinates) && !fromColumn) warn(id, `coordonnées illisibles « ${r.coordonnees ?? r.coordinates} » (format attendu : 36.7538, 2.9949)`)
  let lat = fromColumn ? fromColumn.lat : r.lat ? Number(r.lat) : null
  let lng = fromColumn ? fromColumn.lng : r.lng ? Number(r.lng) : null
  if ((lat === null || lng === null) && r.google_maps_url) {
    const c = await coordsFromMapsUrl(r.google_maps_url)
    if (c) ({ lat, lng } = c)
    else warn(id, 'coordonnées introuvables dans google_maps_url')
  }
  // Garde-fou : on reste dans la région d'Alger
  if (lat !== null && lng !== null && (lat < 36.4 || lat > 37.1 || lng < 2.5 || lng > 3.6)) { warn(id, `coordonnées hors Alger (${lat}, ${lng}), ignorées`); lat = lng = null }
  if (lat === null || lng === null) warn(id, 'lat/lng manquants (nécessaires pour la carte)')

  out.push({
    id, name: r.name, commune, cuisines, priceRange: prices,
    address: orNull(r.address), lat, lng, phone: orNull(r.phone), instagram: handle,
    googleMapsUrl: orNull(r.google_maps_url), googlePlaceId: orNull(r.google_place_id),
    source: ['manual', 'google', 'instagram'].includes(r.source) ? r.source : 'manual',
    ratingAvg: 0, ratingCount: 0,
    logoUrl: `/restaurants/${id}/logo.webp`, photoUrl: `/restaurants/${id}/pdc.webp`,
  })
  for (const f of ['logo', 'pdc']) {
    if (!fs.existsSync(path.join('public/restaurants', id, `${f}.webp`))) warn(id, `image ${f} absente`)
  }
}

fs.writeFileSync('src/data/restaurants.json', JSON.stringify(out, null, 2) + '\n')
console.log(`${out.length} restos écrits dans src/data/restaurants.json`)
if (warnings.length) console.log('Avertissements :\n- ' + warnings.join('\n- '))
