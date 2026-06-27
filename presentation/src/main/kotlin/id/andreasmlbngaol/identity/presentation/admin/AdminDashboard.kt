package id.andreasmlbngaol.identity.presentation.admin

import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.usecase.auth.LoginUseCase
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.plugin.AdminSession
import id.andreasmlbngaol.identity.presentation.security.toRequestContext
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlin.uuid.Uuid
import kotlinx.html.BODY
import kotlinx.html.FormMethod
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

private const val USERS_BASE = "/admin/dashboard/users"

fun Route.adminDashboardRoutes(deps: ApiDependencies, users: UserRepository) {
    route("/admin/dashboard") {
        // --- Authentication endpoints (open) ------------------------------
        get("/login") {
            call.respondText(loginPage(), ContentType.Text.Html, HttpStatusCode.OK)
        }

        post("/login") {
            val params = call.receiveParameters()
            val identifier = params["identifier"].orEmpty().trim()
            val password = params["password"].orEmpty()
            if (identifier.isBlank() || password.isBlank()) {
                return@post call.respondText(
                    loginPage("Email/username and password are required."),
                    ContentType.Text.Html,
                    HttpStatusCode.BadRequest,
                )
            }
            val authenticated = runCatching {
                deps.login.execute(
                    LoginUseCase.Command(identifier = identifier, password = password),
                    call.toRequestContext(),
                )
            }.isSuccess
            if (!authenticated) {
                return@post call.respondText(
                    loginPage("Invalid credentials, or the account is not active/verified."),
                    ContentType.Text.Html,
                    HttpStatusCode.Unauthorized,
                )
            }
            val user = users.findByAnyIdentifier(identifier)
            if (user == null || !user.hasRole("ADMIN")) {
                call.sessions.clear<AdminSession>()
                return@post call.respondText(
                    loginPage("This account does not have administrator access."),
                    ContentType.Text.Html,
                    HttpStatusCode.Forbidden,
                )
            }
            call.sessions.set(AdminSession(userId = user.id.toString(), email = user.email))
            call.respondRedirect("/admin/dashboard")
        }

        post("/logout") {
            call.sessions.clear<AdminSession>()
            call.respondRedirect("/admin/dashboard/login")
        }

        // --- Protected dashboard ------------------------------------------
        get {
            val session = call.sessions.get<AdminSession>()
            if (session == null) {
                return@get call.respondRedirect("/admin/dashboard/login")
            }
            call.sessions.set(session) // sliding expiration: re-issue the cookie on each visit
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1")
                    title { +"Identity — Admin" }
                    script(src = "https://unpkg.com/htmx.org@2.0.3") {}
                    style { unsafe { +DASHBOARD_CSS } }
                }
                body { dashboardBody(session.email) }
            }
        }

        // HTMX fragment: users table.
        get("/fragments/users") {
            if (!call.ensureAdminFragment()) return@get
            call.respondUsersFragment(deps)
        }

        // --- Admin actions (session-guarded; return the refreshed table) ---
        post("/users/{id}/suspend") {
            if (!call.ensureAdminFragment()) return@post
            val id = parseUserId(call.parameters["id"])
                ?: return@post call.respondUsersFragment(deps, "Invalid user id.")
            val result = runCatching { deps.setUserStatus.suspend(id, call.toRequestContext()) }
            call.respondUsersFragment(deps, result.exceptionOrNull()?.userMessage())
        }

        post("/users/{id}/reactivate") {
            if (!call.ensureAdminFragment()) return@post
            val id = parseUserId(call.parameters["id"])
                ?: return@post call.respondUsersFragment(deps, "Invalid user id.")
            val result = runCatching { deps.setUserStatus.reactivate(id, call.toRequestContext()) }
            call.respondUsersFragment(deps, result.exceptionOrNull()?.userMessage())
        }

        post("/users/{id}/delete") {
            if (!call.ensureAdminFragment()) return@post
            val id = parseUserId(call.parameters["id"])
                ?: return@post call.respondUsersFragment(deps, "Invalid user id.")
            val result = runCatching { deps.setUserStatus.softDelete(id, call.toRequestContext()) }
            call.respondUsersFragment(deps, result.exceptionOrNull()?.userMessage())
        }

        post("/users/{id}/reset-password") {
            if (!call.ensureAdminFragment()) return@post
            val id = parseUserId(call.parameters["id"])
                ?: return@post call.respondUsersFragment(deps, "Invalid user id.")
            val result = runCatching {
                val user = deps.getUser.execute(id)
                deps.forgotPassword.execute(user.email, call.toRequestContext())
            }
            val message = result.exceptionOrNull()?.userMessage() ?: "Password reset email sent."
            call.respondUsersFragment(deps, message, isError = result.isFailure)
        }

        post("/users/{id}/roles/{role}/assign") {
            if (!call.ensureAdminFragment()) return@post
            val id = parseUserId(call.parameters["id"])
                ?: return@post call.respondUsersFragment(deps, "Invalid user id.")
            val role = call.parameters["role"].orEmpty()
            val result = runCatching { deps.manageRoles.assign(id, role, call.toRequestContext()) }
            call.respondUsersFragment(deps, result.exceptionOrNull()?.userMessage())
        }

        post("/users/{id}/roles/{role}/revoke") {
            if (!call.ensureAdminFragment()) return@post
            val id = parseUserId(call.parameters["id"])
                ?: return@post call.respondUsersFragment(deps, "Invalid user id.")
            val role = call.parameters["role"].orEmpty()
            val result = runCatching { deps.manageRoles.revoke(id, role, call.toRequestContext()) }
            call.respondUsersFragment(deps, result.exceptionOrNull()?.userMessage())
        }

        // HTMX fragment: recent audit logs.
        get("/fragments/audit") {
            if (!call.ensureAdminFragment()) return@get
            val page = deps.listAuditLogs.execute(PageRequest(page = 0, size = 25))
            call.respondText(contentType = ContentType.Text.Html) {
                buildString {
                    append("<table><thead><tr><th>When</th><th>Action</th><th>User</th><th>IP</th><th>OK</th></tr></thead><tbody>")
                    page.items.forEach { log ->
                        append("<tr>")
                        append("<td>${log.createdAt}</td>")
                        append("<td>${log.action.name}</td>")
                        append("<td>${(log.userId?.toString() ?: "—").escape()}</td>")
                        append("<td>${(log.ipAddress ?: "—").escape()}</td>")
                        append("<td>${if (log.success) "✓" else "✗"}</td>")
                        append("</tr>")
                    }
                    append("</tbody></table>")
                }
            }
        }
    }
}

/**
 * Guards an HTMX fragment: when there is no admin session, instructs HTMX to do
 * a full-page redirect to the login screen and returns false so the caller can
 * stop. Returns true when a valid session is present.
 */
private suspend fun ApplicationCall.ensureAdminFragment(): Boolean {
    val session = sessions.get<AdminSession>()
    if (session != null) {
        // Sliding expiration: every 15s poll re-issues the cookie, so an active
        // admin is never logged out mid-session; idle sessions still expire.
        sessions.set(session)
        return true
    }
    response.headers.append("HX-Redirect", "/admin/dashboard/login")
    respondText("", ContentType.Text.Html, HttpStatusCode.Unauthorized)
    return false
}

/** Renders the users table (optionally with a status/error banner) and responds. */
private suspend fun ApplicationCall.respondUsersFragment(
    deps: ApiDependencies,
    message: String? = null,
    isError: Boolean = true,
) {
    val currentUserId = sessions.get<AdminSession>()?.userId
    val banner = when {
        message == null -> ""
        isError -> """<p class="error">${message.escape()}</p>"""
        else -> """<p class="ok">${message.escape()}</p>"""
    }
    respondText(banner + usersTableHtml(deps, this, currentUserId), ContentType.Text.Html)
}

/** Builds the users table HTML, including per-row admin action buttons. */
private suspend fun usersTableHtml(
    deps: ApiDependencies,
    call: ApplicationCall,
    currentUserId: String?,
): String {
    val page = deps.listUsers.execute(call.pageReq())
    return buildString {
        append("<table><thead><tr><th>Email</th><th>Username</th><th>Status</th><th>Roles</th><th>Verified</th><th>Actions</th></tr></thead><tbody>")
        page.items.forEach { u ->
            val id = u.id.toString()
            val isSelf = currentUserId != null && currentUserId == id
            val isAdmin = u.roleNames.contains("ADMIN")
            append("<tr>")
            append("<td>${u.email.escape()}</td>")
            append("<td>${u.username.escape()}</td>")
            append("<td><span class=\"badge\">${u.status.name}</span></td>")
            append("<td>${u.roleNames.joinToString(", ").escape()}</td>")
            append("<td>${if (u.emailVerified) "✓" else "—"}</td>")
            append("<td class=\"actions\">")
            when (u.status.name) {
                "ACTIVE" -> if (!isSelf) append(actionBtn("Suspend", "$USERS_BASE/$id/suspend", "Suspend this user?"))
                "SUSPENDED" -> append(actionBtn("Reactivate", "$USERS_BASE/$id/reactivate", null))
                else -> {}
            }
            append(actionBtn("Reset password", "$USERS_BASE/$id/reset-password", "Send a password reset email to ${u.email}?"))
            if (isAdmin) {
                if (!isSelf) append(actionBtn("Revoke admin", "$USERS_BASE/$id/roles/ADMIN/revoke", "Revoke admin from this user?"))
            } else {
                append(actionBtn("Make admin", "$USERS_BASE/$id/roles/ADMIN/assign", "Grant admin to this user?"))
            }
            if (u.status.name != "DELETED" && !isSelf) {
                append(actionBtn("Delete", "$USERS_BASE/$id/delete", "Delete this user? This revokes all their sessions."))
            }
            if (isSelf) append("<span class=\"muted self\">(you)</span>")
            append("</td>")
            append("</tr>")
        }
        append("</tbody></table>")
        append("<p class=\"muted\">Total: ${page.total} user(s)</p>")
    }
}

/** Builds one HTMX action button that re-renders the users table on success. */
private fun actionBtn(label: String, url: String, confirm: String?): String {
    val confirmAttr = if (confirm != null) """ hx-confirm="${confirm.escape()}"""" else ""
    val danger = if (label == "Delete" || label == "Revoke admin") " btn-danger" else ""
    return """<button class="btn-sm$danger" hx-post="$url" hx-target="#users" hx-swap="innerHTML"$confirmAttr>$label</button>"""
}

private fun parseUserId(raw: String?): Uuid? =
    raw?.let { runCatching { Uuid.parse(it) }.getOrNull() }

private fun Throwable.userMessage(): String = message ?: "Action failed."

private fun BODY.dashboardBody(email: String) {
    div("container") {
        div("topbar") {
            div {
                h1 { +"Identity Admin" }
                p("muted") { +"Server-rendered control panel. Data refreshes via HTMX." }
            }
            div("session") {
                span("muted") { +email }
                form(action = "/admin/dashboard/logout", method = FormMethod.post) {
                    button(classes = "btn btn-ghost") { +"Log out" }
                }
            }
        }

        div("card") {
            div("card-head") {
                h2 { +"Users" }
                a(href = "#", classes = "btn") {
                    attributes["hx-get"] = "/admin/dashboard/fragments/users"
                    attributes["hx-target"] = "#users"
                    +"Refresh"
                }
            }
            div {
                id = "users"
                attributes["hx-get"] = "/admin/dashboard/fragments/users"
                attributes["hx-trigger"] = "load, every 15s"
                p("muted") { +"Loading…" }
            }
        }

        div("card") {
            div("card-head") {
                h2 { +"Recent activity" }
                a(href = "#", classes = "btn") {
                    attributes["hx-get"] = "/admin/dashboard/fragments/audit"
                    attributes["hx-target"] = "#audit"
                    +"Refresh"
                }
            }
            div {
                id = "audit"
                attributes["hx-get"] = "/admin/dashboard/fragments/audit"
                attributes["hx-trigger"] = "load, every 15s"
                p("muted") { +"Loading…" }
            }
        }
    }
}

private fun ApplicationCall.pageReq(): PageRequest {
    val page = ((request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)) - 1
    val size = request.queryParameters["size"]?.toIntOrNull()?.coerceIn(1, PageRequest.MAX_PAGE_SIZE) ?: 20
    return PageRequest(page = page, size = size, search = request.queryParameters["search"]?.takeIf { it.isNotBlank() })
}

private fun String.escape(): String = this
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

/** Standalone login page (raw HTML; messages are fixed, trusted strings). */
private fun loginPage(error: String? = null): String {
    val errorHtml = if (error != null) """<p class="error">$error</p>""" else ""
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Admin sign in - Identity</title>
        <style>$LOGIN_CSS</style>
        </head>
        <body>
        <div class="card">
        <h1>Admin sign in</h1>
        <p class="muted">Sign in with an administrator account to access the dashboard.</p>
        $errorHtml
        <form method="post" action="/admin/dashboard/login">
        <label>Email or username
        <input name="identifier" required autocomplete="username">
        </label>
        <label>Password
        <input type="password" name="password" required autocomplete="current-password">
        </label>
        <button type="submit">Sign in</button>
        </form>
        </div>
        </body>
        </html>
    """.trimIndent()
}

private const val LOGIN_CSS = """
  :root { color-scheme: light dark; }
  * { box-sizing: border-box; }
  body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
         margin: 0; min-height: 100vh; display: grid; place-items: center;
         background: #0b0c10; color: #e8e8ea; padding: 20px; }
  .card { background: #15171c; border: 1px solid #23262e; border-radius: 14px;
          padding: 32px; max-width: 380px; width: 100%; }
  h1 { font-size: 22px; margin: 0 0 6px; }
  .muted { color: #9aa0aa; font-size: 13px; margin: 0 0 4px; }
  .error { color: #ff8a80; font-size: 13px; margin: 12px 0 0; }
  form { display: flex; flex-direction: column; gap: 14px; margin-top: 18px; }
  label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #9aa0aa; }
  input { background: #0b0c10; border: 1px solid #23262e; border-radius: 8px;
          padding: 10px 12px; color: #e8e8ea; font-size: 14px; }
  button { background: #2a6df4; color: #fff; border: 0; border-radius: 8px;
           padding: 11px 12px; font-size: 14px; cursor: pointer; margin-top: 4px; }
  button:hover { background: #1f5be0; }
"""

private const val DASHBOARD_CSS = """
  :root { color-scheme: light dark; }
  * { box-sizing: border-box; }
  body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; margin: 0; background: #0b0c10; color: #e8e8ea; }
  .container { max-width: 1040px; margin: 0 auto; padding: 32px 20px; }
  .topbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
  .session { display: flex; align-items: center; gap: 12px; }
  .session form { margin: 0; }
  h1 { font-size: 26px; margin: 0 0 4px; }
  h2 { font-size: 17px; margin: 0; }
  .muted { color: #9aa0aa; font-size: 13px; }
  .self { margin-left: 4px; }
  .ok { color: #7ee0a0; font-size: 13px; margin: 0 0 10px; }
  .error { color: #ff8a80; font-size: 13px; margin: 0 0 10px; }
  .card { background: #15171c; border: 1px solid #23262e; border-radius: 12px; padding: 18px; margin-top: 18px; }
  .card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
  table { width: 100%; border-collapse: collapse; font-size: 14px; }
  th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #23262e; vertical-align: middle; }
  th { color: #9aa0aa; font-weight: 600; }
  .badge { background: #1f2a44; color: #93b4ff; padding: 2px 8px; border-radius: 999px; font-size: 12px; }
  .actions { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
  .btn { background: #2a6df4; color: #fff; padding: 6px 12px; border-radius: 8px; text-decoration: none; font-size: 13px; border: 0; cursor: pointer; }
  .btn:hover { background: #1f5be0; }
  .btn-ghost { background: transparent; border: 1px solid #23262e; color: #c7ccd4; }
  .btn-ghost:hover { background: #1a1d24; }
  .btn-sm { background: #1f2a44; color: #cdd9ff; border: 1px solid #2c3a5e; border-radius: 7px; padding: 4px 9px; font-size: 12px; cursor: pointer; }
  .btn-sm:hover { background: #28324f; }
  .btn-danger { background: #3a1620; color: #ff8a80; border-color: #5e2c34; }
  .btn-danger:hover { background: #4a1c28; }
"""
