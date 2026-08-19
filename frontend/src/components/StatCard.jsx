function StatCard({ label, value, sublabel, variant = 'used' }) {
  return (
    <article className={`stat-card stat-card--${variant}`}>
      <p className="stat-label">{label}</p>
      <p className="stat-value">{value}</p>
      <p className="stat-sublabel">{sublabel}</p>
    </article>
  )
}

export default StatCard
