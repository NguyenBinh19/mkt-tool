export function StatCard({ label, value, accent }) {
  return <article className={`stat ${accent}`}><span>{label}</span><strong>{value}</strong><i /></article>;
}
