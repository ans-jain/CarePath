const ML_BASE_URL = import.meta.env.VITE_ML_URL !== undefined
  ? import.meta.env.VITE_ML_URL
  : (import.meta.env.PROD ? '' : 'http://127.0.0.1:8000');

const BACKEND_BASE_URL = import.meta.env.VITE_BACKEND_URL !== undefined
  ? import.meta.env.VITE_BACKEND_URL
  : (import.meta.env.PROD ? '/api/v1' : 'http://127.0.0.1:8080/api/v1');

export class ApiError extends Error {
  status: number;
  data: any;

  constructor(message: string, status: number, data?: any) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('carepath_access_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function requestJson<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = {
    'Content-Type': 'application/json',
    ...getAuthHeader(),
    ...(options.headers || {}),
  };

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    let data: any = null;
    if (typeof response.text === 'function') {
      const rawText = await response.text();
      if (rawText && rawText.trim().length > 0) {
        try {
          data = JSON.parse(rawText);
        } catch {
          data = rawText;
        }
      }
    } else if (typeof response.json === 'function') {
      try {
        data = await response.json();
      } catch {
        data = null;
      }
    }

    if (!response.ok) {
      let errorMessage = 'An unexpected server error occurred.';
      if (response.status === 404) {
        errorMessage = 'Endpoint not found (404). Service is unreachable or not deployed on this host.';
      } else if (response.status === 503) {
        errorMessage = 'Model service is initializing or model is not loaded yet.';
      } else if (typeof data === 'object' && data !== null) {
        if (Array.isArray(data.validationErrors) && data.validationErrors.length > 0) {
          // Spring Boot field validation errors
          errorMessage = data.validationErrors
            .map((err: any) => err.rule || err.message || `${err.field} is invalid`)
            .join('; ');
        } else if (data.message) {
          errorMessage = String(data.message);
        } else if (data.detail) {
          if (Array.isArray(data.detail)) {
            // Pydantic validation errors
            errorMessage = data.detail
              .map((d: any) => {
                const field = Array.isArray(d.loc) && d.loc.length > 0 ? d.loc[d.loc.length - 1] : 'Field';
                return `${field}: ${d.msg}`;
              })
              .join(', ');
          } else {
            errorMessage = String(data.detail);
          }
        }
      } else if (typeof data === 'string' && data.length > 0) {
        errorMessage = data;
      }

      throw new ApiError(errorMessage, response.status, data);
    }

    if (data === null) {
      // In static CDN deployments (e.g. Render Static Sites), POST to an API path returns 200 with 0 bytes.
      throw new ApiError('Service returned an empty response. Backend is unreachable on this host.', response.status);
    }

    return data as T;
  } catch (err: any) {
    if (err instanceof ApiError) {
      throw err;
    }
    // Network or CORS failure
    throw new ApiError(
      err.message || 'Network connection failed. Ensure the ML and backend services are running.',
      0
    );
  }
}

export { ML_BASE_URL, BACKEND_BASE_URL };
