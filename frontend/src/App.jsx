import { useCallback, useEffect, useRef, useState } from 'react'
import Header from './components/Header.jsx'
import StatCard from './components/StatCard.jsx'
import UsageCard from './components/UsageCard.jsx'
import StatusBadge from './components/StatusBadge.jsx'
import TestRequest from './components/TestRequest.jsx'
import RequestHistory from './components/RequestHistory.jsx'
import { getRateLimitStatus, sendTestRequest } from './services/api.js'
import './App.css'

function Skeleton() {
  return (
    <div className="skeleton" aria-hidden="true">
      <div className="skeleton-stats">
        <div className="skeleton-card" />
        <div className="skeleton-card" />
        <div className="skeleton-card" />
      </div>
      <div className="skeleton-panel" />
    </div>
  )
}

function App() {
  const [status, setStatus] = useState(null)
  const [connection, setConnection] = useState('checking')
  const [countdown, setCountdown] = useState(0)
  const [lastResult, setLastResult] = useState(null)
  const [history, setHistory] = useState([])
  const [sending, setSending] = useState(false)
  const inFlightRef = useRef(false)

  const loadStatus = useCallback(async () => {
    if (inFlightRef.current) return
    inFlightRef.current = true
    try {
      const data = await getRateLimitStatus()
      setConnection('connected')
      setStatus(data)
      setCountdown(data.resetInSeconds)
    } catch {
      setConnection('disconnected')
    } finally {
      inFlightRef.current = false
    }
  }, [])

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown((prev) => (prev > 0 ? prev - 1 : 0))
      loadStatus()
    }, 1000)
    const initialTimer = setTimeout(loadStatus, 0)
    return () => {
      clearInterval(timer)
      clearTimeout(initialTimer)
    }
  }, [loadStatus])

  const handleSendTest = async () => {
    if (sending) return
    setSending(true)
    try {
      const result = await sendTestRequest()
      setLastResult(result)
      setHistory((prev) =>
        [
          {
            id: crypto.randomUUID(),
            method: 'GET',
            endpoint: '/api/test',
            status: result.status,
            time: new Date(),
          },
          ...prev,
        ].slice(0, 10),
      )
    } catch {
      setLastResult({ status: 0, body: null, error: true })
    } finally {
      setSending(false)
      loadStatus()
    }
  }

  return (
    <div className="app-shell">
      <Header connection={connection} />
      <main className="dashboard">
        {connection === 'disconnected' && (
          <div className="banner banner--error" role="status">
            Backend Disconnected — unable to connect to backend.
          </div>
        )}

        {status ? (
          <>
            <section className="stats-grid" aria-label="Rate limit statistics">
              <StatCard label="LIMIT" value={status.limit} sublabel="requests / window" variant="limit" />
              <StatCard label="USED" value={status.used} sublabel="requests consumed" variant="used" />
              <StatCard label="REMAINING" value={status.remaining} sublabel="requests available" variant="remaining" />
            </section>

            <div className="content-grid">
              <div className="content-left">
                <UsageCard
                  used={status.used}
                  limit={status.limit}
                  resetInSeconds={countdown}
                  status={status.status}
                />
                <TestRequest sending={sending} onSend={handleSendTest} result={lastResult} />
              </div>
              <aside className="content-right">
                <StatusBadge status={status.status} />
                <RequestHistory history={history} />
              </aside>
            </div>
          </>
        ) : (
          <Skeleton />
        )}
      </main>
    </div>
  )
}

export default App