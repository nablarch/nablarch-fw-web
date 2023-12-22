<html>
  <head>
    <title>Greeting Service</title>
  </head>
  <body>
    <% jakarta.servlet.http.HttpServletRequest req = null; %>
    <%= req.getAttribute("greeting") %>
  </body>
</html>
