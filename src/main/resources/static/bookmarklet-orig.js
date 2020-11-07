$.ajax({
    url: "%HOST%/add-feed",
    dataType: "jsonp",
    data: {
        url: window.location.href
    }
})
.done(function (data) {
    alert(data.title || data.error);
});

