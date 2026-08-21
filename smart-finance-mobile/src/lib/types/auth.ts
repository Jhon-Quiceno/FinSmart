export interface ApiUserResponse {
  id: number;
  name: string;
  email: string;
}

export interface ApiAuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: ApiUserResponse;
  /** Solo presente cuando el request llevo X-Client: mobile (AuthResponse es @JsonInclude(NON_NULL) en el backend). */
  refreshToken?: string;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}
