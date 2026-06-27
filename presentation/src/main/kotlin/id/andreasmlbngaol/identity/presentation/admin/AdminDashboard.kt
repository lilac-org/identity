package id.andreasmlbngaol.identity.presentation.admin

import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

/**
 * A lightweight, server-rendered admin dashboard (kotlinx.html + HTMX).
 *
 * Authentication for the dashboard is expected to be handled by a reverse proxy
 * or an SSO layer in front of the service (the JSON API remains the source of
 * truth and is independently secured). The HTMX fragments below read directly
 * from the use cases so no client build step is required.
 */
fun Route.adminDashboardRoutes(deps: ApiDependencies) {
    route("/admin/dashboard") {
        get {
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1")
                    title { +"Identity — Admin" }
                    script(src = "https://unpkg.com/htmx.org@2.0.3") {}
                    style { unsafe { +DASHBOARD_CSS } }
                }
                body { dashboardBody() }
            }
        }

        // HTMX fragment: users table.
        get("/fragments/users") {
            val page = deps.listUsers.execute(call.pageReq())
            call.respondText(contentType = ContentType.Text.Html) {
                buildString {
                    append("<table><thead><tr><th>Email</th><th>Username</th><th>Status</th><th>Roles</th><th>Verified</th></tr></thead><tbody>")
                    page.items.forEach { u ->
                        append("<tr>")
                        append("<td>${u.email.escape()}</td>")
                        append("<td>${u.username.escape()}</td>")
                        append("<td><span class=\"badge\">${u.status.name}</span></td>")
                        append("<td>${u.roleNames.joinToString(", ").escape()}</td>")
                        append("<td>${if (u.emailVerified) "✓" else "—"}</td>")
                        append("</tr>")
                    }
                    append("</tbody></table>")
                    append("<p class=\"muted\">Total: ${page.total} user(s)</p>")
                }
            }
        }

        // HTMX fragment: recent audit logs.
        get("/fragments/audit") {
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

private fun BODY.dashboardBody() {
    div("container") {
        h1 { +"Identity Admin" }
        p("muted") { +"Server-rendered control panel. Data refreshes via HTMX." }

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
                attributes["hx-trigger"] = "load"
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
                attributes["hx-trigger"] = "load"
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

private const val DASHBOARD_CSS = """
  :root { color-scheme: light dark; }
  * { box-sizing: border-box; }
  body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; margin: 0; background: #0b0c10; color: #e8e8ea; }
  .container { max-width: 1040px; margin: 0 auto; padding: 32px 20px; }
  h1 { font-size: 26px; margin: 0 0 4px; }
  h2 { font-size: 17px; margin: 0; }
  .muted { color: #9aa0aa; font-size: 13px; }
  .card { background: #15171c; border: 1px solid #23262e; border-radius: 12px; padding: 18px; margin-top: 18px; }
  .card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
  table { width: 100%; border-collapse: collapse; font-size: 14px; }
  th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #23262e; }
  th { color: #9aa0aa; font-weight: 600; }
  .badge { background: #1f2a44; color: #93b4ff; padding: 2px 8px; border-radius: 999px; font-size: 12px; }
  .btn { background: #2a6df4; color: #fff; padding: 6px 12px; border-radius: 8px; text-decoration: none; font-size: 13px; }
  .btn:hover { background: #1f5be0; }
"""
