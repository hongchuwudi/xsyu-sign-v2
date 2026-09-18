package com.hongchu.qqrobotsign.utils;

import com.hongchu.qqrobotsign.enums.CasErrorType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XSYULoginUtilTest {

    @Test
    void shouldTreatMsg1PasswordErrorAsWrongPasswordEvenWhenPageContainsDisabled() {
        String html = """
                <html>
                  <script>document.querySelector('button').disabled = true;</script>
                  <span id="msg1">账号或密码错误。</span>
                </html>
                """;

        assertEquals(CasErrorType.WRONG_PASSWORD, XSYULoginUtil.diagnoseError(html));
        assertEquals("账号或密码错误。", XSYULoginUtil.extractErrorMessage(html));
    }

    @Test
    void shouldTreatExplicitLockedMessageAsAccountLocked() {
        String html = """
                <html>
                  <span class="errors" id='msg1'>您的账号被锁定，请联系管理员。</span>
                </html>
                """;

        assertEquals(CasErrorType.ACCOUNT_LOCKED, XSYULoginUtil.diagnoseError(html));
    }

    @Test
    void shouldRequireCaptchaWhenAuthcodeFieldIsActive() {
        String html = """
                <html>
                  <span id="msg1">账号或密码错误。</span>
                  <input type="text" name="authcode" />
                </html>
                """;

        assertEquals(CasErrorType.CAPTCHA_REQUIRED, XSYULoginUtil.diagnoseError(html));
    }

    @Test
    void shouldKeepUnknownCasMessageAsOtherError() {
        String html = """
                <html>
                  <span id="msg1">认证服务繁忙，请稍后再试。</span>
                </html>
                """;

        assertEquals(CasErrorType.OTHER_ERROR, XSYULoginUtil.diagnoseError(html));
        assertEquals("认证服务繁忙，请稍后再试。", XSYULoginUtil.extractErrorMessage(html));
    }

    @Test
    void shouldReadLegacyMsgNodeAndNormalizeNestedMarkup() {
        String html = """
                <html>
                  <span data-source="cas" id='msg'><strong>账号或密码错误。</strong>&nbsp;</span>
                </html>
                """;

        assertEquals("账号或密码错误。", XSYULoginUtil.extractErrorMessage(html));
        assertEquals(CasErrorType.WRONG_PASSWORD, XSYULoginUtil.diagnoseError(html));
    }
}
