"use client"

import { useState, useRef, useEffect } from "react"
import { AppLayout } from "@/components/layout/app-layout"
import { 
  Sparkles, 
  Send, 
  TrendingDown, 
  PiggyBank, 
  AlertCircle, 
  Lightbulb,
  Target,
  Bot,
  User,
  ArrowRight,
  Zap,
  Shield,
  Clock
} from "lucide-react"
import { cn } from "@/lib/utils"

interface Insight {
  id: number
  title: string
  message: string
  type: "warning" | "tip" | "insight" | "action"
  icon: React.ReactNode
  metric?: string
  change?: string
}

const insights: Insight[] = [
  {
    id: 1,
    title: "Gasto Elevado en Alimentacion",
    message: "Estas gastando un 25% mas en alimentacion que el mes pasado. Considera revisar tus habitos de compra o buscar alternativas mas economicas.",
    type: "warning",
    icon: <AlertCircle className="h-5 w-5" />,
    metric: "$4,250",
    change: "+25%"
  },
  {
    id: 2,
    title: "Oportunidad de Ahorro",
    message: "Si reduces $500 en entretenimiento, podrias alcanzar tu meta de ahorro este mes y acumular $2,000 adicionales al ano.",
    type: "tip",
    icon: <Lightbulb className="h-5 w-5" />,
    metric: "$500",
    change: "Meta alcanzable"
  },
  {
    id: 3,
    title: "Excelente Ratio de Ahorro",
    message: "Tu ratio de ahorro actual es del 15%. Esta por encima del promedio recomendado del 10%. Sigue asi para alcanzar tus metas financieras.",
    type: "insight",
    icon: <PiggyBank className="h-5 w-5" />,
    metric: "15%",
    change: "+5% vs promedio"
  },
  {
    id: 4,
    title: "Suscripcion Sin Uso",
    message: "Detectamos un gasto recurrente de $299 en una suscripcion que no has usado en 2 meses. Considera cancelarla para ahorrar $3,588 al ano.",
    type: "action",
    icon: <TrendingDown className="h-5 w-5" />,
    metric: "$299/mes",
    change: "2 meses sin uso"
  },
  {
    id: 5,
    title: "Meta de Ahorro Cercana",
    message: "Estas al 85% de tu meta de ahorro mensual. Con solo $750 mas alcanzaras tu objetivo. Te recomendamos reducir gastos variables.",
    type: "insight",
    icon: <Target className="h-5 w-5" />,
    metric: "85%",
    change: "Faltan $750"
  },
]

interface Message {
  id: number
  role: "user" | "assistant"
  content: string
  timestamp: Date
}

const initialMessages: Message[] = [
  {
    id: 1,
    role: "assistant",
    content: "Hola! Soy tu asistente financiero personal. Puedo ayudarte a analizar tus gastos, crear presupuestos, darte consejos de ahorro y responder cualquier pregunta sobre tus finanzas. Como puedo ayudarte hoy?",
    timestamp: new Date()
  }
]

const suggestedQuestions = [
  "Como puedo reducir mis gastos mensuales?",
  "Cuanto deberia ahorrar cada mes?",
  "Cual es mi mayor gasto este mes?",
  "Como puedo pagar mis deudas mas rapido?"
]

export default function AsistenteIAPage() {
  const [messages, setMessages] = useState<Message[]>(initialMessages)
  const [inputValue, setInputValue] = useState("")
  const [isTyping, setIsTyping] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = (content: string) => {
    if (!content.trim()) return

    const userMessage: Message = {
      id: messages.length + 1,
      role: "user",
      content: content.trim(),
      timestamp: new Date()
    }

    setMessages(prev => [...prev, userMessage])
    setInputValue("")
    setIsTyping(true)

    // Simulate AI response
    setTimeout(() => {
      const responses: Record<string, string> = {
        "Como puedo reducir mis gastos mensuales?": "Basandome en tu historial financiero, te recomiendo:\n\n1. **Alimentacion ($4,250)**: Considera planificar tus comidas semanalmente y comprar al mayoreo. Podrias ahorrar hasta $800/mes.\n\n2. **Suscripciones ($1,200)**: Tienes 3 suscripciones que no usas frecuentemente. Cancelarlas te ahorraria $450/mes.\n\n3. **Transporte ($2,100)**: Evalua opciones de transporte compartido o publico para reducir costos.\n\nEn total, podrias ahorrar aproximadamente $1,500 mensuales.",
        "Cuanto deberia ahorrar cada mes?": "Segun la regla 50/30/20 y tu ingreso mensual de $25,000:\n\n- **Necesidades (50%)**: $12,500\n- **Deseos (30%)**: $7,500\n- **Ahorro (20%)**: $5,000\n\nActualmente estas ahorrando $3,750 (15%), lo cual es bueno pero podrias mejorar. Te sugiero automatizar transferencias a tu cuenta de ahorro el dia de pago.",
        "Cual es mi mayor gasto este mes?": "Tu mayor categoria de gasto este mes es **Alimentacion** con $4,250, representando el 28% de tus gastos totales.\n\nTus top 5 categorias son:\n1. Alimentacion: $4,250 (+25% vs mes anterior)\n2. Vivienda: $3,500 (sin cambios)\n3. Transporte: $2,100 (+5%)\n4. Servicios: $1,800 (sin cambios)\n5. Entretenimiento: $1,500 (+12%)\n\nTe recomiendo enfocarte en reducir alimentacion y entretenimiento este mes.",
        "Como puedo pagar mis deudas mas rapido?": "Analizando tus deudas actuales, te recomiendo el metodo **Avalancha**:\n\n1. **Tarjeta de Credito** (18% interes): Prioriza esta deuda primero\n2. **Prestamo Personal** (12% interes): Segunda prioridad\n3. **Credito Auto** (8% interes): Menor prioridad\n\nSi destinas $2,000 extra al mes a la tarjeta de credito, la pagaras en 8 meses en lugar de 24, ahorrando $3,200 en intereses."
      }

      const response = responses[content] || `Entiendo tu pregunta sobre "${content}". Basandome en tu perfil financiero actual:\n\n- Ingresos mensuales: $25,000\n- Gastos promedio: $15,200\n- Ahorro actual: 15%\n- Deuda total: $45,000\n\nPuedo darte recomendaciones mas especificas si me cuentas mas detalles sobre tu situacion o meta financiera.`

      const assistantMessage: Message = {
        id: messages.length + 2,
        role: "assistant",
        content: response,
        timestamp: new Date()
      }

      setMessages(prev => [...prev, assistantMessage])
      setIsTyping(false)
    }, 1500)
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage(inputValue)
    }
  }

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">Asistente IA</h1>
          <p className="text-muted-foreground mt-1">
            Insights inteligentes y asistencia financiera personalizada
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-5">
          {/* Insights Section */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
                <Sparkles className="h-5 w-5 text-primary" />
              </div>
              <h2 className="text-lg font-semibold text-foreground">Insights Financieros</h2>
            </div>

            <div className="space-y-3 max-h-[calc(100vh-280px)] overflow-y-auto pr-2">
              {insights.map((insight) => (
                <div
                  key={insight.id}
                  className={cn(
                    "rounded-xl border p-4 transition-all hover:scale-[1.02] cursor-pointer",
                    insight.type === "warning" && "border-warning/30 bg-warning/5 hover:border-warning/50",
                    insight.type === "tip" && "border-primary/30 bg-primary/5 hover:border-primary/50",
                    insight.type === "insight" && "border-success/30 bg-success/5 hover:border-success/50",
                    insight.type === "action" && "border-destructive/30 bg-destructive/5 hover:border-destructive/50"
                  )}
                >
                  <div className="flex items-start gap-3">
                    <div
                      className={cn(
                        "flex h-10 w-10 items-center justify-center rounded-lg shrink-0",
                        insight.type === "warning" && "bg-warning/10 text-warning",
                        insight.type === "tip" && "bg-primary/10 text-primary",
                        insight.type === "insight" && "bg-success/10 text-success",
                        insight.type === "action" && "bg-destructive/10 text-destructive"
                      )}
                    >
                      {insight.icon}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <h3 className="font-semibold text-foreground text-sm">{insight.title}</h3>
                        {insight.metric && (
                          <span className={cn(
                            "text-xs font-medium px-2 py-0.5 rounded-full",
                            insight.type === "warning" && "bg-warning/20 text-warning",
                            insight.type === "tip" && "bg-primary/20 text-primary",
                            insight.type === "insight" && "bg-success/20 text-success",
                            insight.type === "action" && "bg-destructive/20 text-destructive"
                          )}>
                            {insight.metric}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-muted-foreground mt-1 leading-relaxed">
                        {insight.message}
                      </p>
                      {insight.change && (
                        <span className={cn(
                          "inline-flex items-center gap-1 text-xs mt-2 font-medium",
                          insight.type === "warning" && "text-warning",
                          insight.type === "tip" && "text-primary",
                          insight.type === "insight" && "text-success",
                          insight.type === "action" && "text-destructive"
                        )}>
                          <ArrowRight className="h-3 w-3" />
                          {insight.change}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-3 gap-3 pt-2">
              <div className="rounded-lg bg-card border border-border p-3 text-center">
                <Zap className="h-5 w-5 text-primary mx-auto mb-1" />
                <p className="text-xs text-muted-foreground">Insights</p>
                <p className="text-lg font-bold text-foreground">5</p>
              </div>
              <div className="rounded-lg bg-card border border-border p-3 text-center">
                <Shield className="h-5 w-5 text-success mx-auto mb-1" />
                <p className="text-xs text-muted-foreground">Score</p>
                <p className="text-lg font-bold text-foreground">78</p>
              </div>
              <div className="rounded-lg bg-card border border-border p-3 text-center">
                <Clock className="h-5 w-5 text-warning mx-auto mb-1" />
                <p className="text-xs text-muted-foreground">Alertas</p>
                <p className="text-lg font-bold text-foreground">2</p>
              </div>
            </div>
          </div>

          {/* Chat Section */}
          <div className="lg:col-span-3 flex flex-col rounded-xl bg-card border border-border overflow-hidden h-[calc(100vh-200px)]">
            {/* Chat Header */}
            <div className="flex items-center gap-3 p-4 border-b border-border bg-muted/30">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/60">
                <Bot className="h-5 w-5 text-primary-foreground" />
              </div>
              <div>
                <h3 className="font-semibold text-foreground">Asistente Financiero</h3>
                <div className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-success animate-pulse" />
                  <span className="text-xs text-muted-foreground">En linea - Listo para ayudarte</span>
                </div>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={cn(
                    "flex gap-3",
                    message.role === "user" && "flex-row-reverse"
                  )}
                >
                  <div className={cn(
                    "flex h-8 w-8 items-center justify-center rounded-full shrink-0",
                    message.role === "assistant" 
                      ? "bg-gradient-to-br from-primary to-primary/60" 
                      : "bg-muted"
                  )}>
                    {message.role === "assistant" 
                      ? <Bot className="h-4 w-4 text-primary-foreground" />
                      : <User className="h-4 w-4 text-muted-foreground" />
                    }
                  </div>
                  <div className={cn(
                    "max-w-[80%] rounded-2xl px-4 py-3",
                    message.role === "assistant"
                      ? "bg-muted/50 rounded-tl-sm"
                      : "bg-primary text-primary-foreground rounded-tr-sm"
                  )}>
                    <p className={cn(
                      "text-sm whitespace-pre-line leading-relaxed",
                      message.role === "assistant" ? "text-foreground" : "text-primary-foreground"
                    )}>
                      {message.content}
                    </p>
                    <p className={cn(
                      "text-[10px] mt-1",
                      message.role === "assistant" ? "text-muted-foreground" : "text-primary-foreground/70"
                    )}>
                      {message.timestamp.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit" })}
                    </p>
                  </div>
                </div>
              ))}

              {isTyping && (
                <div className="flex gap-3">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/60 shrink-0">
                    <Bot className="h-4 w-4 text-primary-foreground" />
                  </div>
                  <div className="bg-muted/50 rounded-2xl rounded-tl-sm px-4 py-3">
                    <div className="flex gap-1">
                      <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce" style={{ animationDelay: "0ms" }} />
                      <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce" style={{ animationDelay: "150ms" }} />
                      <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce" style={{ animationDelay: "300ms" }} />
                    </div>
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Suggested Questions */}
            {messages.length === 1 && (
              <div className="px-4 pb-2">
                <p className="text-xs text-muted-foreground mb-2">Preguntas sugeridas:</p>
                <div className="flex flex-wrap gap-2">
                  {suggestedQuestions.map((question, index) => (
                    <button
                      key={index}
                      onClick={() => handleSendMessage(question)}
                      className="text-xs bg-muted/50 hover:bg-muted text-foreground px-3 py-1.5 rounded-full transition-colors border border-border hover:border-primary/30"
                    >
                      {question}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Input */}
            <div className="p-4 border-t border-border bg-muted/20">
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  onKeyDown={handleKeyPress}
                  placeholder="Escribe tu pregunta financiera..."
                  className="flex-1 bg-background border border-border rounded-xl px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all"
                  disabled={isTyping}
                />
                <button
                  onClick={() => handleSendMessage(inputValue)}
                  disabled={!inputValue.trim() || isTyping}
                  className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                >
                  <Send className="h-5 w-5" />
                </button>
              </div>
              <p className="text-[10px] text-muted-foreground text-center mt-2">
                El asistente utiliza IA para darte consejos basados en tus datos financieros
              </p>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
