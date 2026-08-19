function UsageCard({ used, limit, resetInSeconds, status }) {
  const safeLimit = limit > 0 ? limit : 1
  const percent = Math.min(100, Math.round((used / safeLimit) * 100))
  const reached = status === 'LIMIT_REACHED'
  const resetLabel = resetInSeconds > 0 ? `Resets in ${resetInSeconds}s` : 'No active window'

  return (
    <section className="panel usage-card" aria-labelledby="usage-title">
      <div className="panel-header">
        <h2 id="usage-title" className="panel-title">
          Current Window
        </h2>
        <span className="usage-count">
          {used} / {limit} requests
        </span>
      </div>
      <div
        className="progress"
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin="0"
        aria-valuemax="100"
        aria-label={`Rate limit usage ${percent} percent`}
      >
        <div className={`progress-fill${reached ? ' progress-fill--limited' : ''}`} style={{ width: `${percent}%` }} />
      </div>
      <div className="usage-footer">
        <span className="usage-percent">{percent}% used</span>
        <span className="usage-reset">{resetLabel}</span>
      </div>
    </section>
  )
}

export default UsageCard
