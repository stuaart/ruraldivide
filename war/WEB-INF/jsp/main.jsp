<%@ page isELIgnored="false" contentType="text/html" pageEncoding="windows-1252"%>
<%@ taglib prefix="spring"  uri="http://www.springframework.org/tags" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">


<html>
<head>
<title>Rural Divide server</title>
</head>

<body>
	<h2>Upload log files:</h2>
	<div>
		<form action='upload/single' method='POST' enctype='multipart/form-data'>
			<div>Single entry log file: <input type='file' name='singleentry'>
			<input type='submit' value='Go'>
			</div>
		</form>

		<form action='upload/bulk' method='POST' enctype='multipart/form-data'>
			<div>Compressed log file: <input type='file' name='compressedfile'>
			<input type='submit' value='Go'>
			</div>
		</form>
		<form action='upload/bulk' method='POST' enctype='multipart/form-data'>
			<div>Uncompressed log file: <input type='file' name='uncompressedfile'>
			<input type='submit' value='Go'>
			</div>
		</form>
		<form action='upload/bulk' method='POST' enctype='multipart/form-data'>
			<div>GZipped log file: <input type='file' name='gzippedfile'>
			<input type='submit' value='Go'>
			</div>
		</form>
	</div>
	<h2>Clean PostGIS db</h2>
	<div>
		<form action='cleandb' method='POST'>
			<div><input type='submit' value='Clean'></div>
		</form>
	</div>
</body>
</html>
