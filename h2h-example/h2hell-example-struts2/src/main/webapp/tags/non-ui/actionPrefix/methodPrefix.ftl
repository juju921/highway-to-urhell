

<html>
<head>
	<title>Showcase - Tags - Non UI - Action Prefix (freemarker)</title>
</head>
<body>
	
	You have come to this page because you used an 'method' prefix.<p/>
	
	The text you've enter is ${text?default('')?html}<p/>
	
	<@s.url id="url" action="actionPrefixExampleUsingFreemarker" namespace="/tags/non-ui/actionPrefix" />
	<@s.a href="%{#url}">Back</@s.a>
	
</body>
</html>


