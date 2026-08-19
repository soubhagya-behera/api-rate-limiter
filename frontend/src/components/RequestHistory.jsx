function RequestHistory({ history }) {
  return (
    <section className="panel history-card" aria-labelledby="history-title">
      <h2 id="history-title" className="panel-title">
        Recent Requests
      </h2>
      {history.length === 0 ? (
        <p className="history-empty">No test requests yet. Send a request to populate this list.</p>
      ) : (
        <ul className="history-list">
          <li className="history-head" aria-hidden="true">
            <span>Method</span>
            <span>Endpoint</span>
            <span>HTTP</span>
            <span>Result</span>
            <span>Time</span>
          </li>
          {history.map((item) => {
            const allowed = item.status === 200
            const tone = allowed ? 'allowed' : 'limited'
            const resultLabel = allowed ? 'Allowed' : item.status === 429 ? 'Rate Limited' : 'Failed'
            return (
              <li className="history-row" key={item.id}>
                <span className="history-method">GET</span>
                <span className="history-endpoint">{item.endpoint}</span>
                <span className={`history-status history-status--${tone}`}>{item.status}</span>
                <span className={`history-result-label history-result-label--${tone}`}>{resultLabel}</span>
                <span className="history-time">
                  {item.time.toLocaleTimeString([], { hour12: false })}
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

export default RequestHistory
