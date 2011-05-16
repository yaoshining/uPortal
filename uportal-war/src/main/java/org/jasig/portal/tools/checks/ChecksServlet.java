/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.tools.checks;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A Servlet which renders the results of the checks.
 * @version $Revision$ $Date$
 * @since uPortal 2.5
 */
public class ChecksServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String RERUN_PARAM = "rerun";

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (RERUN_PARAM.equals(request.getParameter(RERUN_PARAM))) {
            final ServletContext servletContext = getServletContext();
            final WebApplicationContext webAppCtx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
            final InitializingCheckRunner checkRunner = (InitializingCheckRunner)webAppCtx.getBean("initializingCheckRunning", InitializingCheckRunner.class);
            servletContext.setAttribute(InitializingCheckRunner.INIT_CHECK_RESULTS, checkRunner);
        }

        doGet(request, response);
    }

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        @SuppressWarnings("unchecked")
        final List<CheckAndResult> results = (List<CheckAndResult>) this.getServletContext().getAttribute(InitializingCheckRunner.INIT_CHECK_RESULTS);

        response.setHeader("pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate");
        response.setDateHeader("Expires", 0);

        PrintWriter writer = response.getWriter();

        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>");
        writer.println("uPortal instrumentation");
        writer.println("</title>");
        writer.println("</head>");

        writer.println("<body>");

        if (results == null) {
            writer.println("<p>Could not find check results in servlet context.</p>");
        }
        else {
            List<CheckAndResult> failedChecks = failedChecks(results);

            if (!failedChecks.isEmpty()) {
                writer.println("<h3>Failed checks</h3>");
                writer.println("<p>" + failedChecks.size() + " checks failed.</p>");

                printChecksAsTable(failedChecks, writer);

            }

            writer.println("<h3>Results of all checks:</h3>");

            printChecksAsTable(results, writer);

            writer.println("<form action='instrumentation' method='post'>");
            writer.println("<input name='rerun' value='rerun' type='submit'/>");
            writer.println("</form>");

        }

        // print out package information

        writer.println("<br/>");
        writer.println("<h3>Package information.</h3>");

        writer.println("<table>");
        writer.println("<tr>");
        writer.println("<td>");
        writer.println("Implementation title");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Implementation version");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Package name");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Implementation vendor");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Specification title");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Specification vendor");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Specification version");
        writer.println("</td>");
        writer.println("</tr>");

        Package[] packs = Package.getPackages();

        for (int i = 0; i < packs.length; i++) {
            Package pack = packs[i];
            writer.println("<tr>");
            writer.println("<td>");
            writer.println(pack.getImplementationTitle());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(pack.getImplementationVersion());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(pack.getName());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(pack.getImplementationVendor());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(pack.getSpecificationTitle());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(pack.getSpecificationVendor());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(pack.getSpecificationVersion());
            writer.println("</td>");
            writer.println("</tr>");
        }

        writer.println("</table>");

        writer.println("</body>");

        writer.println("</html>");
        writer.flush();

    }

    /**
     * Returns the List of failed checks that contains, order retained, those
     * checks from the given list that failed.
     * @param checks a List of CheckAndResults
     * @return a List of those of the given CheckAndResults that failed.
     */
    private List<CheckAndResult> failedChecks(List<CheckAndResult> checks) {
        if (checks == null) {
            throw new IllegalArgumentException("Checks must not be null.");
        }

        List<CheckAndResult> failedChecks = new ArrayList<CheckAndResult>();

        for (final CheckAndResult checkAndResult : checks) {
            if (!checkAndResult.isSuccess()) {
                // found a failure
                failedChecks.add(checkAndResult);
            }
        }

        return failedChecks;
    }

    /**
     * Prints the given List of CheckAndResult instances as a table to the
     * given PrintWriter.
     * @param checks CheckAndResult instances
     * @param writer output target
     */
    private void printChecksAsTable(List<CheckAndResult> checks, PrintWriter writer) {
        writer.println("<table>");
        writer.println("<tr>");
        writer.println("<td>");
        writer.println("Status");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Check description");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Check result message");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("Remediation advice");
        writer.println("</td>");
        writer.println("</tr>");

        for (final CheckAndResult checkAndResult : checks) {
            writer.println("<tr>");
            writer.println("<td>");
            if (checkAndResult.isSuccess()) {
                writer.println("OK");
            }
            else {
                writer.println("FAILURE");
            }
            writer.println("</td>");
            writer.println("<td>");
            writer.println(checkAndResult.getCheckDescription());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(checkAndResult.getResult().getMessage());
            writer.println("</td>");
            writer.println("<td>");
            if (checkAndResult.isSuccess()) {
                writer.println("Not applicable.");
            }
            else {
                writer.println(checkAndResult.getResult().getRemediationAdvice());
            }
            writer.println("</td>");
            writer.println("</tr>");

        }

        writer.println("</table>");
    }

}