import { context, propagation, SpanKind, SpanStatusCode, trace } from '@opentelemetry/api'
import { NodeTracerProvider } from '@opentelemetry/sdk-trace-node'
import fetch, { Headers } from 'node-fetch'

const provider = new NodeTracerProvider()
provider.register()
const tracer = trace.getTracer('msinit')

export function log(level, message, error) {
    const spanContext = trace.getActiveSpan()?.spanContext()
    console.log(JSON.stringify({
        '@timestamp': new Date().toISOString(),
        serviceName: 'msinit',
        level,
        message,
        ...(spanContext ? { traceId: spanContext.traceId, spanId: spanContext.spanId } : {}),
        ...(error ? { error: error.stack || String(error) } : {})
    }))
}

export function traceRequest(req, res, next) {
    const parent = propagation.extract(context.active(), req.headers)
    tracer.startActiveSpan(req.method + ' ' + req.path, { kind: SpanKind.SERVER }, parent, span => {
        res.setHeader('X-Trace-ID', span.spanContext().traceId)
        const requestContext = context.active()
        let ended = false

        const finish = () => {
            if(ended)
                return
            ended = true
            context.with(requestContext, () => {
                if(res.statusCode >= 500)
                    span.setStatus({ code: SpanStatusCode.ERROR })
                log('INFO', req.method + ' ' + req.path + ' ' + res.statusCode)
            })
            span.end()
        }

        res.once('finish', finish)
        res.once('close', finish)
        next()
    })
}

export function tracedFetch(url, options = {}) {
    return tracer.startActiveSpan('HTTP ' + (options.method || 'GET'), { kind: SpanKind.CLIENT }, async span => {
        const headers = new Headers(options.headers)
        propagation.inject(context.active(), headers, {
            set: (carrier, name, value) => carrier.set(name, value)
        })

        try {
            const response = await fetch(url, { ...options, headers })
            if(response.status >= 400)
                span.setStatus({ code: SpanStatusCode.ERROR })
            return response
        } catch(error) {
            span.recordException(error)
            span.setStatus({ code: SpanStatusCode.ERROR })
            throw error
        } finally {
            span.end()
        }
    })
}
