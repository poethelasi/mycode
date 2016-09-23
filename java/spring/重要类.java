/**
 * Servlet Filter that allows one to specify a character encoding for requests.
 * This is useful because current browsers typically do not set a character
 * encoding even if specified in the HTML page or form.
 *
 * <p>This filter can either apply its encoding if the request does not already
 * specify an encoding, or enforce this filter's encoding in any case
 * ("forceEncoding"="true"). In the latter case, the encoding will also be
 * applied as default response encoding (although this will usually be overridden
 * by a full content type set in the view).
 */
提交表单时无法指定characterEncoding，就算制定了也无效。
<form enctype="application/x-www-urlencode;charset">这样指定是没用的。
org.springframework.web.filter.CharacterEncodingFilter
