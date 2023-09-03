package internal

import (
	"fmt"
	"path/filepath"

	// "github.com/gin-contrib/pprof"
	"github.com/apisix/manager-api/internal/conf"
	"github.com/apisix/manager-api/internal/filter"
	"github.com/apisix/manager-api/internal/handler"
	"github.com/apisix/manager-api/internal/handler/authentication"
	"github.com/apisix/manager-api/internal/handler/consumer"
	"github.com/apisix/manager-api/internal/handler/data_loader"
	"github.com/apisix/manager-api/internal/handler/global_rule"
	"github.com/apisix/manager-api/internal/handler/healthz"
	"github.com/apisix/manager-api/internal/handler/label"
	"github.com/apisix/manager-api/internal/handler/migrate"
	"github.com/apisix/manager-api/internal/handler/plugin_config"
	"github.com/apisix/manager-api/internal/handler/proto"
	"github.com/apisix/manager-api/internal/handler/route"
	"github.com/apisix/manager-api/internal/handler/schema"
	"github.com/apisix/manager-api/internal/handler/server_info"
	"github.com/apisix/manager-api/internal/handler/service"
	"github.com/apisix/manager-api/internal/handler/ssl"
	"github.com/apisix/manager-api/internal/handler/stream_route"
	"github.com/apisix/manager-api/internal/handler/system_config"
	"github.com/apisix/manager-api/internal/handler/tool"
	"github.com/apisix/manager-api/internal/handler/upstream"
	"github.com/apisix/manager-api/internal/log"
	"github.com/gin-contrib/gzip"
	"github.com/gin-contrib/static"
	"github.com/gin-gonic/gin"
)

func SetUpRouter() *gin.Engine {
	if conf.ENV == conf.EnvLOCAL || conf.ENV == conf.EnvDEV {
		gin.SetMode(gin.DebugMode)
	} else {
		gin.SetMode(gin.ReleaseMode)
	}
	r := gin.New()
	logger := log.GetLogger(log.AccessLog)
	// security
	r.Use(filter.RequestLogHandler(logger), filter.IPFilter(), filter.InvalidRequest())

	// authenticate
	if conf.OidcEnabled {
		r.Use(filter.Oidc())
	}
	r.Use(filter.Authentication())

	// misc
	r.Use(gzip.Gzip(gzip.DefaultCompression), filter.CORS(), filter.RequestId(), filter.SchemaCheck(), filter.RecoverHandler())
	r.Use(static.Serve("/", static.LocalFile(filepath.Join(conf.WorkDir, conf.WebDir), false)))
	r.NoRoute(func(c *gin.Context) {
		c.File(fmt.Sprintf("%s/index.html", filepath.Join(conf.WorkDir, conf.WebDir)))
	})

	factories := []handler.RegisterFactory{
		route.NewHandler,
		ssl.NewHandler,
		consumer.NewHandler,
		upstream.NewHandler,
		service.NewHandler,
		schema.NewHandler,
		schema.NewSchemaHandler,
		healthz.NewHandler,
		authentication.NewHandler,
		global_rule.NewHandler,
		server_info.NewHandler,
		label.NewHandler,
		data_loader.NewHandler,
		data_loader.NewImportHandler,
		tool.NewHandler,
		plugin_config.NewHandler,
		migrate.NewHandler,
		proto.NewHandler,
		stream_route.NewHandler,
		system_config.NewHandler,
	}

	for i := range factories {
		h, err := factories[i]()
		if err != nil {
			panic(err)
		}
		h.ApplyRoute(r)
	}

	// pprof.Register(r)

	return r
}