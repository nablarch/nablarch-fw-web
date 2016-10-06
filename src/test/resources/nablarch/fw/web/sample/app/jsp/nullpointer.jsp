<html>
  <head>
    <title>Greeting Service</title>
  </head>
  <body>
    <% javax.servlet.http.HttpServletRequest req = null; %>
    <%= req.getAttribute("greeting") %>
  </body>
</html>
