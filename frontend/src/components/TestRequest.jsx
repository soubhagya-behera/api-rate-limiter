function ResultPanel({ result }) {
  if (!result) return null

  if (result.error || result.status === 0) {
    return (
      <div className="result result--limited" role="status">
        <p className="result-title">Request Failed</p>
        <p className="result-meta">Unable to connect to backend.</p>
      </div>
    )
  }

  if (result.status === 200) {
    return (
      <div className="result result--allowed" role="status">
        <p className="result-title">Request Allowed</p>
        <p className="result-meta">HTTP {result.status}</p>
        <p className="result-detail">Remaining: {result.body?.remainingRequests ?? '—'}</p>
      </div>
    )
  }

  if (result.status === 429) {
    return (
      <div className="result result--limited" role="status">
        <p className="result-title">Rate Limit Reached</p>
        <p className="result-meta">HTTP {result.status}</p>
        <p className="result-detail">Retry after: {result.body?.retryAfterSeconds ?? '—'} seconds</p>
      </div>
    )
  }

  return (
    <div className="result result--limited" role="status">
      <p className="result-title">Request Failed</p>
      <p className="result-meta">HTTP {result.status}</p>
    </div>
  )
}

function TestRequest({ sending, onSend, result }) {
  return (
    <section className="panel test-card" aria-labelledby="test-title">
      <h2 id="test-title" className="panel-title">
        Test API Endpoint
      </h2>
      <p className="test-description">
        Send a request to the protected endpoint and observe the rate limiter in real time.
      </p>
      <button
        type="button"
        className="button button--primary"
        onClick={onSend}
        disabled={sending}
      >
        {sending ? 'Sending...' : 'Send Test Request'}
      </button>
      <ResultPanel result={result} />
    </section>
  )
}

export default TestRequest
