/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebVM.
 *
 * WebVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebVM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebVM.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.rss;

import sk.baka.webvm.ProblemReport;
import sk.baka.webvm.Problems;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides a RSS feed with the "Problems" report.
 * @author Martin Vysny
 */
public final class RssFeed extends HttpServlet {

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/rss+xml");
        PrintWriter out = response.getWriter();
        try {
            out.println("<?xml version=\"1.0\"?>\n<rss version=\"2.0\">");
            out.println("  <channel>\n    <title>SysInfo feeds</title>\n    <link>.</link>\n    <description>SysInfo: Remote server problems</description>");
            out.println("    <language>en-us</language>\n    <ttl>1</ttl>\n");
            final List<ProblemReport> problems = Problems.getProblems();
            if (ProblemReport.isProblem(problems)) {
                out.print("    <item>\n      <title>SysInfo: Problems report for ");
                out.print(new Date());
                out.print("</title>\n      <link>Problems.html</link>\n      <description>Problems report:&lt;br/&gt;&lt;br/&gt;");
                out.print(ProblemReport.toString(Problems.getProblems(), "&lt;br/&gt;"));
                out.print("</description>\n      <pubDate>");
                out.print(new Date());
                out.print("</pubDate>\n      <guid>");
                out.print(System.currentTimeMillis());
                out.println("</guid>\n    </item>");
            }
            out.println("  </channel>\n</rss>\n");
        } finally {
            out.close();
        }
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "RSS Feed";
    }
}
