import { NextRequest, NextResponse } from "next/server"

const PUBLIC_ROUTES = ["/login", "/registro", "/recuperar-password"]

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl
  const refreshToken = request.cookies.get("financeai_refresh_token")?.value
  const isPublicRoute = PUBLIC_ROUTES.includes(pathname)

  if (!refreshToken && !isPublicRoute) {
    const loginUrl = new URL("/login", request.url)
    return NextResponse.redirect(loginUrl)
  }

  if (refreshToken && isPublicRoute) {
    const appUrl = new URL("/", request.url)
    return NextResponse.redirect(appUrl)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\.).*)"],
}
