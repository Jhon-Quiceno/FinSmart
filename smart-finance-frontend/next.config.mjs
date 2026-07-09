/** @type {import('next').NextConfig} */
const nextConfig = {
  /**
   * Proxy all /api calls through the frontend's own origin to the backend.
   * This makes the auth cookies (refresh token + XSRF-TOKEN) first-party in
   * production (Vercel domain), so the browser stores them and JavaScript can
   * read the CSRF cookie — cross-domain cookies between *.vercel.app and
   * *.run.app are otherwise unreadable/blocked (Safari, incognito).
   *
   * BACKEND_API_URL is a server-side env var (set it in Vercel to the Cloud
   * Run URL). Locally it defaults to the Spring Boot dev server.
   */
  async rewrites() {
    const backendUrl = process.env.BACKEND_API_URL ?? "http://localhost:8080"

    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ]
  },
}

export default nextConfig
