<%@ page session="true"%>
<html>
	<head>
		<title>display</title>
	</head>
	<body>
		
		<p>Getting session data from session</p>
		
		<p>Data is expected to be "gigaspaces". If data is "null", no session could be read</p>
		
		<p>Data: <%=(String)session.getAttribute("name")%></p>
		
	</body>
</html>