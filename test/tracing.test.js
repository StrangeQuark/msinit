import test from 'node:test'
import assert from 'node:assert/strict'
import express from 'express'
import { trace } from '@opentelemetry/api'
import { traceRequest, tracedFetch } from '../tracing.js'

test('continues the incoming trace through asynchronous work and outbound HTTP', async () => {
    const app = express()
    app.use(traceRequest)
    app.get('/downstream', (req, res) => res.json({ parent: req.headers.traceparent }))
    app.get('/request', async (req, res) => {
        await new Promise(resolve => setImmediate(resolve))
        const response = await tracedFetch(baseUrl + '/downstream')
        res.json({ current: trace.getActiveSpan().spanContext().traceId, downstream: await response.json() })
    })
    const server = app.listen(0, '127.0.0.1')
    await new Promise(resolve => server.once('listening', resolve))
    const baseUrl = 'http://127.0.0.1:' + server.address().port
    try {
        const traceId = '4bf92f3577b34da6a3ce929d0e0e4736'
        const response = await fetch(baseUrl + '/request', {
            headers: { traceparent: '00-' + traceId + '-00f067aa0ba902b7-01' }
        })
        assert.equal(response.headers.get('X-Trace-ID'), traceId)
        const body = await response.json()
        assert.equal(body.current, traceId)
        assert.ok(body.downstream.parent.startsWith('00-' + traceId + '-'))
        assert.equal(trace.getActiveSpan(), undefined)

        const error = await fetch(baseUrl + '/missing', { headers: { traceparent: 'invalid' } })
        assert.equal(error.status, 404)
        assert.match(error.headers.get('X-Trace-ID'), /^[0-9a-f]{32}$/)
        assert.notEqual(error.headers.get('X-Trace-ID'), traceId)
    } finally {
        server.closeAllConnections()
        await new Promise(resolve => server.close(resolve))
    }
})
