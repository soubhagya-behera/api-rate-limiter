const STATUS_META = {
  READY: { label: 'READY', tone: 'ready' },
  ACTIVE: { label: 'ACTIVE', tone: 'active' },
  LIMIT_REACHED: { label: 'LIMIT_REACHED', tone: 'limit' },
}

function StatusBadge({ status }) {
  const meta = STATUS_META[status] || { label: status || 'UNKNOWN', tone: 'unknown' }

  return (
    <section className="panel status-card" aria-labelledby="status-title">
      <h2 id="status-title" className="panel-title">
        Status
      </h2>
      <span className={`status-badge status-badge--${meta.tone}`}>
        <span className="status-dot" aria-hidden="true" />
        {meta.label}
      </span>
    </section>
  )
}

export default StatusBadge
