# Frontend CSRF integration

The API uses JWT bearer authentication and cookie-based CSRF protection. CSRF does
not replace the `Authorization: Bearer <accessToken>` header for protected routes.

1. Fetch `GET /api/v1/authentication/csrf` before login. The response contains
   `headerName` (`X-XSRF-TOKEN`) and `token`, and sets an HttpOnly `XSRF-TOKEN` cookie.
2. Keep the returned token in memory. Send it using the returned header name on
   POST, PUT, PATCH, and DELETE requests, including login and logout. The browser
   must send the cookie too. GET, HEAD, OPTIONS, and TRACE do not require the header.
3. Successful login clears the pre-login CSRF cookie. Fetch the CSRF endpoint
   again before making another state-changing request.
4. Logout is `POST /api/v1/authentication/logout` and also requires a matching
   cookie and header. After logout, discard the JWT and fetch a fresh CSRF token
   before logging in again.

Use the token from the JSON response, not the raw cookie: Spring masks the response
token for BREACH protection. The cookie remains HttpOnly. Fetching the endpoint
again can return a different masked representation of the same underlying token;
it does not invalidate the previously returned representation by itself.

Example for a frontend served from the same origin as the API:

```javascript
let csrf;

async function refreshCsrf() {
  const response = await fetch('/api/v1/authentication/csrf', {
    credentials: 'same-origin',
    cache: 'no-store'
  });
  if (!response.ok) throw new Error('Unable to obtain CSRF token');
  csrf = await response.json();
}

await refreshCsrf();
const response = await fetch('/api/v1/authentication/login', {
  method: 'POST',
  credentials: 'same-origin',
  headers: {
    'Content-Type': 'application/json',
    [csrf.headerName]: csrf.token
  },
  body: JSON.stringify({ email, password })
});
if (!response.ok) throw new Error(`Login failed: ${response.status}`);
const { accessToken } = await response.json();
await refreshCsrf();
// Subsequent writes also include Authorization: `Bearer ${accessToken}`.
```

Missing or mismatched CSRF tokens return `403`. Missing or invalid JWTs on protected
routes return `401` once CSRF validation has passed. A token header alone is not
sufficient without its matching cookie.

## Lifetime

There is no fixed server-enforced CSRF token expiration or embedded expiry claim.
The cookie has no persistent Max-Age, so it normally lasts for the browser session
(browser session restoration can retain it). Login and logout clear the browser's
cookie. The next CSRF fetch creates a new token if the cookie is absent.
JWT expiration is independent. Cookie deletion is not a server-side revocation
list for previously copied cookie/token pairs.

The cookie uses SameSite=Lax and becomes Secure over HTTPS. This setup assumes a
same-origin frontend, such as one behind the same reverse proxy. A frontend on a
different origin needs an explicit trusted-origin CORS configuration and
`credentials: 'include'`; cross-site deployments also need a deliberate cookie
SameSite/Secure configuration. Do not enable wildcard credentialed CORS.

Reference: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
