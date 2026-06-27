package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.usecase.account.ResetPasswordUseCase
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.security.toRequestContext
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.verificationPages(deps: ApiDependencies) {
    // --- Email verification ----------------------------------------------
    // GET shows a confirmation page with a button; it does NOT verify.
    get("/verify-email") {
        val token = call.request.queryParameters["token"].orEmpty()
        if (token.isBlank()) {
            return@get call.respondText(
                resultPage("Email verification", ok = false, messageHtml = "Missing verification token."),
                ContentType.Text.Html,
                HttpStatusCode.BadRequest,
            )
        }
        call.respondText(verifyConfirmPage(token), ContentType.Text.Html, HttpStatusCode.OK)
    }

    // POST performs the actual verification when the user clicks the button.
    post("/verify-email") {
        val token = call.receiveParameters()["token"].orEmpty()
        if (token.isBlank()) {
            return@post call.respondText(
                resultPage("Email verification", ok = false, messageHtml = "Missing verification token."),
                ContentType.Text.Html,
                HttpStatusCode.BadRequest,
            )
        }
        runCatching { deps.verifyEmail.execute(token, call.toRequestContext()) }.fold(
            onSuccess = { user ->
                call.respondText(
                    resultPage(
                        "Email verified",
                        ok = true,
                        messageHtml = "Your email <strong>${user.email.escapeHtml()}</strong> has been verified. " +
                            "You can now sign in.",
                    ),
                    ContentType.Text.Html,
                    HttpStatusCode.OK,
                )
            },
            onFailure = { e ->
                call.respondText(
                    resultPage("Email verification", ok = false, messageHtml = (e.message ?: "Verification failed").escapeHtml()),
                    ContentType.Text.Html,
                    HttpStatusCode.BadRequest,
                )
            },
        )
    }

    // --- Password reset ---------------------------------------------------
    // GET shows the form; POST applies the new password.
    get("/reset-password") {
        val token = call.request.queryParameters["token"].orEmpty()
        call.respondText(resetFormPage(token), ContentType.Text.Html, HttpStatusCode.OK)
    }

    post("/reset-password") {
        val params = call.receiveParameters()
        val token = params["token"].orEmpty()
        val password = params["password"].orEmpty()
        val confirm = params["confirm"].orEmpty()
        when {
            token.isBlank() -> call.respondText(
                resultPage("Reset password", ok = false, messageHtml = "Missing reset token."),
                ContentType.Text.Html,
                HttpStatusCode.BadRequest,
            )
            password.isBlank() || password != confirm -> call.respondText(
                resetFormPage(token, error = "Passwords do not match."),
                ContentType.Text.Html,
                HttpStatusCode.BadRequest,
            )
            else -> runCatching {
                deps.resetPassword.execute(ResetPasswordUseCase.Command(token, password), call.toRequestContext())
            }.fold(
                onSuccess = {
                    call.respondText(
                        resultPage(
                            "Password updated",
                            ok = true,
                            messageHtml = "Your password has been reset. You can now sign in with your new password.",
                        ),
                        ContentType.Text.Html,
                        HttpStatusCode.OK,
                    )
                },
                onFailure = { e ->
                    call.respondText(
                        resetFormPage(token, error = (e.message ?: "Reset failed").escapeHtml()),
                        ContentType.Text.Html,
                        HttpStatusCode.BadRequest,
                    )
                },
            )
        }
    }
}

/** Minimal HTML escaping for values interpolated into the pages. */
private fun String.escapeHtml(): String = this
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

/** Confirmation page shown for GET /verify-email; verifies only on button POST. */
private fun verifyConfirmPage(token: String): String {
    val safeToken = token.escapeHtml()
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Confirm email - Identity</title>
        <style>$PAGE_CSS</style>
        </head>
        <body>
        <div class="card">
        <h1>Confirm your email</h1>
        <p>Click the button below to verify your email address and activate your account.</p>
        <form method="post" action="/verify-email">
        <input type="hidden" name="token" value="$safeToken">
        <button type="submit">Verify my email</button>
        </form>
        </div>
        </body>
        </html>
    """.trimIndent()
}

/** A simple success/error result page. messageHtml is treated as trusted HTML. */
private fun resultPage(heading: String, ok: Boolean, messageHtml: String): String {
    val icon = if (ok) "\u2713" else "\u2717"
    val accent = if (ok) "#2ecc71" else "#e74c3c"
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>$heading - Identity</title>
        <style>$PAGE_CSS</style>
        </head>
        <body>
        <div class="card">
        <div class="icon" style="color:$accent">$icon</div>
        <h1>$heading</h1>
        <p>$messageHtml</p>
        </div>
        </body>
        </html>
    """.trimIndent()
}

/** The reset-password form page, optionally showing an error. */
private fun resetFormPage(token: String, error: String? = null): String {
    val errorHtml = if (error != null) """<p class="error">$error</p>""" else ""
    val safeToken = token.escapeHtml()
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Reset password - Identity</title>
        <style>$PAGE_CSS</style>
        </head>
        <body>
        <div class="card">
        <h1>Reset your password</h1>
        $errorHtml
        <form method="post" action="/reset-password">
        <input type="hidden" name="token" value="$safeToken">
        <label>New password
        <input type="password" name="password" required minlength="8" autocomplete="new-password">
        </label>
        <label>Confirm password
        <input type="password" name="confirm" required minlength="8" autocomplete="new-password">
        </label>
        <button type="submit">Update password</button>
        </form>
        </div>
        </body>
        </html>
    """.trimIndent()
}

private const val PAGE_CSS = """
  :root { color-scheme: light dark; }
  * { box-sizing: border-box; }
  body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
         margin: 0; min-height: 100vh; display: grid; place-items: center;
         background: #0b0c10; color: #e8e8ea; padding: 20px; }
  .card { background: #15171c; border: 1px solid #23262e; border-radius: 14px;
          padding: 32px; max-width: 420px; width: 100%; text-align: center; }
  .icon { font-size: 44px; line-height: 1; margin-bottom: 8px; }
  h1 { font-size: 22px; margin: 0 0 12px; }
  p { color: #c7ccd4; font-size: 14px; line-height: 1.5; }
  .error { color: #ff8a80; }
  form { display: flex; flex-direction: column; gap: 14px; margin-top: 18px; text-align: left; }
  label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #9aa0aa; }
  input { background: #0b0c10; border: 1px solid #23262e; border-radius: 8px;
          padding: 10px 12px; color: #e8e8ea; font-size: 14px; }
  button { background: #2a6df4; color: #fff; border: 0; border-radius: 8px;
           padding: 11px 12px; font-size: 14px; cursor: pointer; margin-top: 4px; }
  button:hover { background: #1f5be0; }
"""