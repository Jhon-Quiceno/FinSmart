import { NextRequest, NextResponse } from "next/server"

const PUBLIC_ROUTES = ["/login", "/registro", "/recuperar-password"]

function isPublicRoute(pathname: string): boolean {
  return PUBLIC_ROUTES.includes(pathname)
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl
  const refreshToken = request.cookies.get("financeai_refresh_token")?.value
  const publicRoute = isPublicRoute(pathname)

  if (!refreshToken && !publicRoute) {
    return NextResponse.redirect(new URL("/login", request.url))
  }

  if (refreshToken && publicRoute) {
    return NextResponse.redirect(new URL("/", request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\.).*)"],
}
