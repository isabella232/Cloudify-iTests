<%@page import="org.openspaces.example.data.ejb.SimpleSession,test.data.Data" %>
<html> <body> <br /> <%
try {
	String operationType = new String(request.getParameter("gp1"));
	javax.naming.InitialContext initial = new javax.naming.InitialContext();
	SimpleSession gsSession = (SimpleSession) initial.lookup("simplesession-remote/SimpleSession/local");
	
	// write 10 Data objects into remote space
	if (operationType.equals("Init")) {
		for (int i = 1; i <= 10; i++) {
			gsSession.write(Integer.toString(i));
		} %>
		<span> Wrote 10 data objects to space </span> <%
	} 
	// Make sure space has changed accordingly to test: "..."
	else if (operationType.equals("Check")) {
		int count = gsSession.count();
		Data updatedData = gsSession.read("1"); 
		Data newData = gsSession.read("11"); %>
		<span> Object count: <%= count %> </span><br />
		<span> Data ID 1: <%= updatedData.getData() %></span><br />
		<span> Data ID 11: <%= newData.getData() %></span><br /> <%
	}
	initial.close();
} catch (Exception e) {
	System.out.println("Caught Exception:" + e.getMessage());
	e.printStackTrace();
	e.printStackTrace(response.getWriter());
} %> 

<form name="simpleSessionForm" action="TestFormHandler.jsp" method="get">
<table border="1">
	<tbody>
		<tr>
			<td><input type="radio" name="gp1" value="Init"></td>
			<td>init</td>
		</tr>
		<tr>
			<td><input type="radio" name="gp1" value="Check"></td>
			<td>check</td>
		</tr>
</table>
<input name="button" value="Go" type="submit">Go!</button>
</form>

</body> </html>
