export interface ApiUser {
  id: number
  name: string
  email: string
}

export interface ApiAuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: ApiUser
}
