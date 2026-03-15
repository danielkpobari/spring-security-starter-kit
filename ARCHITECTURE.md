# Architecture Diagram

# Architecture Overview

This document describes the high-level architecture and request flow
of the FCMB Security Assessment application.

The focus is on authentication, authorization, and separation of concerns,
demonstrating production-grade Spring Security design.


## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATION                          │
│                    (Browser, Mobile App, etc.)                      │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ HTTP Requests
                                 │ (JSON + JWT Token)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SAMPLE APPLICATION                             │
│                    (Spring Boot Application)                        │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    REST CONTROLLERS                          │ │
│  │  • AuthController    (/api/auth/login)                       │ │
│  │  • PublicController  (/api/public/*)                         │ │
│  │  • UserController    (/api/user/*)                           │ │
│  │  • AdminController   (/api/admin/*)                          │ │
│  └────────────────────────┬─────────────────────────────────────┘ │
│                           │                                         │
│  ┌────────────────────────▼─────────────────────────────────────┐ │
│  │                    SERVICE LAYER                             │ │
│  │  • AuthService (UserDetailsService)                          │ │
│  │    - loadUserByUsername()                                    │ │
│  │    - login()                                                 │ │
│  └────────────────────────┬─────────────────────────────────────┘ │
│                           │                                         │
│  ┌────────────────────────▼─────────────────────────────────────┐ │
│  │                  REPOSITORY LAYER                            │ │
│  │  • UserRepository (JpaRepository)                            │ │
│  └────────────────────────┬─────────────────────────────────────┘ │
│                           │                                         │
│  ┌────────────────────────▼─────────────────────────────────────┐ │
│  │                    ENTITY LAYER                              │ │
│  │  • User (id, username, password, roles)                      │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 │ Uses (Maven Dependency)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   CORE SECURITY STARTER                             │
│                (Reusable Spring Boot Starter)                       │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │              SPRING SECURITY FILTER CHAIN                    │ │
│  │                                                              │ │
│  │  1. JwtAuthenticationFilter (OncePerRequestFilter)          │ │
│  │     • Extract JWT from Authorization header                 │ │
│  │     • Validate token                                        │ │
│  │     • Set Authentication in SecurityContext                 │ │
│  │     • Log authenticated requests                            │ │
│  │                                                              │ │
│  │  2. UsernamePasswordAuthenticationFilter                    │ │
│  │  3. Authorization Filters                                   │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    JWT COMPONENTS                            │ │
│  │  • JwtTokenProvider                                          │ │
│  │    - generateToken(Authentication)                           │ │
│  │    - validateToken(String)                                   │ │
│  │    - getUsernameFromToken(String)                            │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                 SECURITY CONFIGURATION                       │ │
│  │  • SecurityConfig                                            │ │
│  │    - SecurityFilterChain                                     │ │
│  │    - PasswordEncoder (BCrypt)                                │ │
│  │    - AuthenticationManager                                   │ │
│  │  • SecurityProperties                                        │ │
│  │    - JWT secret, expiration, header                          │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                  EXCEPTION HANDLERS                          │ │
│  │  • CustomAuthenticationEntryPoint (401)                      │ │
│  │  • CustomAccessDeniedHandler (403)                           │ │
│  │  • GlobalExceptionHandler                                    │ │
│  │  • ErrorResponse (standardized format)                       │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │               AUTO-CONFIGURATION                             │ │
│  │  • SecurityAutoConfiguration                                 │ │
│  │  • META-INF/spring/...AutoConfiguration.imports              │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Request Flow Diagram

### 1. Login Flow
```
Client                  Controller              Service                 Starter
  │                        │                       │                       │
  │  POST /api/auth/login  │                       │                       │
  ├───────────────────────>│                       │                       │
  │  {username, password}  │                       │                       │
  │                        │  login(request)       │                       │
  │                        ├──────────────────────>│                       │
  │                        │                       │  authenticate()       │
  │                        │                       ├──────────────────────>│
  │                        │                       │  (AuthenticationMgr)  │
  │                        │                       │                       │
  │                        │                       │  loadUserByUsername() │
  │                        │                       │<──────────────────────┤
  │                        │                       │  UserDetails          │
  │                        │                       │                       │
  │                        │                       │  generateToken()      │
  │                        │                       ├──────────────────────>│
  │                        │                       │  (JwtTokenProvider)   │
  │                        │                       │<──────────────────────┤
  │                        │                       │  JWT Token            │
  │                        │  LoginResponse        │                       │
  │                        │<──────────────────────┤                       │
  │  200 OK                │                       │                       │
  │  {token, username}     │                       │                       │
  │<───────────────────────┤                       │                       │
  │                        │                       │                       │
```

### 2. Protected Endpoint Access Flow
```
Client                  Filter                  Controller              Starter
  │                        │                       │                       │
  │  GET /api/user/me      │                       │                       │
  │  Authorization: Bearer │                       │                       │
  ├───────────────────────>│                       │                       │
  │                        │  Extract JWT          │                       │
  │                        │  validateToken()      │                       │
  │                        ├──────────────────────────────────────────────>│
  │                        │                       │  (JwtTokenProvider)   │
  │                        │<──────────────────────────────────────────────┤
  │                        │  Valid                │                       │
  │                        │                       │                       │
  │                        │  getUsernameFromToken()                       │
  │                        ├──────────────────────────────────────────────>│
  │                        │<──────────────────────────────────────────────┤
  │                        │  "john"               │                       │
  │                        │                       │                       │
  │                        │  Set SecurityContext  │                       │
  │                        │  Log request          │                       │
  │                        │                       │                       │
  │                        │  Continue filter chain│                       │
  │                        ├──────────────────────>│                       │
  │                        │                       │  getCurrentUser()     │
  │                        │                       │                       │
  │                        │  200 OK               │                       │
  │                        │  {username: "john"}   │                       │
  │<───────────────────────┴───────────────────────┤                       │
  │                        │                       │                       │
```

### 3. Authorization Failure Flow (403)
```
Client                  Filter                  Controller              Handler
  │                        │                       │                       │
  │  GET /api/admin/users  │                       │                       │
  │  Authorization: Bearer │                       │                       │
  │  (USER token)          │                       │                       │
  ├───────────────────────>│                       │                       │
  │                        │  Validate token ✓     │                       │
  │                        │  Set SecurityContext  │                       │
  │                        │  (ROLE_USER)          │                       │
  │                        │                       │                       │
  │                        │  Continue filter chain│                       │
  │                        ├──────────────────────>│                       │
  │                        │                       │  @PreAuthorize        │
  │                        │                       │  hasRole('ADMIN') ✗   │
  │                        │                       │                       │
  │                        │                       │  AccessDeniedException│
  │                        │                       ├──────────────────────>│
  │                        │                       │  (AccessDeniedHandler)│
  │                        │                       │                       │
  │  403 Forbidden         │                       │                       │
  │  {error: "Forbidden"}  │                       │                       │
  │<───────────────────────┴───────────────────────┴───────────────────────┤
  │                        │                       │                       │
```

## Module Dependency Graph

```
┌─────────────────────────┐
│   sample-application    │
│                         │
│  • Controllers          │
│  • Services             │
│  • Repositories         │
│  • Entities             │
│  • DTOs                 │
└───────────┬─────────────┘
            │
            │ depends on
            │ (Maven)
            ▼
┌─────────────────────────┐
│  core-security-starter  │
│                         │
│  • Security Config      │
│  • JWT Provider         │
│  • Filters              │
│  • Exception Handlers   │
│  • Auto-Configuration   │
└─────────────────────────┘
```

## Security Layer Interaction

```
┌────────────────────────────────────────────────────────────┐
│                    HTTP REQUEST                            │
└────────────────────────┬───────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────┐
│              JwtAuthenticationFilter                       │
│  • Extract JWT from header                                 │
│  • Validate token signature & expiration                   │
│  • Extract username from token                             │
│  • Create Authentication object                            │
│  • Set in SecurityContext                                  │
└────────────────────────┬───────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────┐
│              Spring Security Filter Chain                  │
│  • Check URL patterns (/api/public/**, /api/admin/**)     │
│  • Verify authentication exists                            │
│  • Check method-level security (@PreAuthorize)             │
└────────────────────────┬───────────────────────────────────┘
                         │
                         ▼
                    ┌────┴────┐
                    │ Allowed? │
                    └────┬────┘
                         │
            ┌────────────┴────────────┐
            │                         │
            ▼ YES                     ▼ NO
┌───────────────────────┐   ┌─────────────────────────┐
│   Controller Method   │   │   Exception Handler     │
│   • Execute logic     │   │   • 401 (no auth)       │
│   • Return response   │   │   • 403 (no permission) │
└───────────────────────┘   └─────────────────────────┘
```

## Configuration Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Startup                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Spring Boot Auto-Configuration                             │
│  • Reads META-INF/spring/...AutoConfiguration.imports       │
│  • Discovers SecurityAutoConfiguration                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  SecurityAutoConfiguration                                  │
│  • @EnableConfigurationProperties(SecurityProperties)       │
│  • @ComponentScan("com.fcmb.security")                      │
│  • Creates ObjectMapper bean                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  SecurityConfig                                             │
│  • Creates SecurityFilterChain                              │
│  • Configures URL patterns                                  │
│  • Registers JwtAuthenticationFilter                        │
│  • Sets up exception handlers                               │
│  • Creates PasswordEncoder (BCrypt)                         │
│  • Creates AuthenticationManager                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Component Scanning                                         │
│  • JwtTokenProvider                                         │
│  • JwtAuthenticationFilter                                  │
│  • CustomAuthenticationEntryPoint                           │
│  • CustomAccessDeniedHandler                                │
│  • GlobalExceptionHandler                                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Application Ready                                          │
│  • All security beans configured                            │
│  • Filter chain active                                      │
│  • Ready to accept requests                                 │
└─────────────────────────────────────────────────────────────┘
```

## Key Design Patterns

1. **Starter Pattern**: Reusable auto-configured library
2. **Filter Chain Pattern**: Request processing pipeline
3. **Strategy Pattern**: Different authentication strategies
4. **Factory Pattern**: Token creation and validation
5. **Template Method Pattern**: OncePerRequestFilter
6. **Dependency Injection**: Spring IoC container
7. **Repository Pattern**: Data access abstraction
8. **DTO Pattern**: Data transfer objects

---

This architecture ensures:
- Clean separation of concerns
-  Reusability across projects
-  Testability
-  Maintainability
-  Scalability
-  Security best practices
