package system_config

import (
	"errors"
	"testing"

	"github.com/apisix/manager-api/internal/core/entity"
	"github.com/apisix/manager-api/internal/core/store"
	"github.com/shiningrush/droplet"
	"github.com/shiningrush/droplet/data"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

func TestSystem_Get(t *testing.T) {
	t.Parallel()
	type testCase struct {
		caseDesc  string
		giveInput *GetInput
		wantErr   error
		wantRet   interface{}
		mockStore store.Interface
		mockFunc  func(tc *testCase)
	}

	cases := []*testCase{
		{
			caseDesc:  "system config not found",
			giveInput: &GetInput{ConfigName: "grafana"},
			wantErr:   data.ErrNotFound,
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("Get", mock.Anything, mock.Anything).Return(nil, tc.wantErr)
				tc.mockStore = mockStore
			},
		},
		{
			caseDesc:  "get system config success",
			giveInput: &GetInput{ConfigName: "grafana"},
			wantErr:   nil,
			wantRet: entity.SystemConfig{
				ConfigName: "grafana",
				Payload: map[string]interface{}{
					"url": "http://127.0.0.1:3000",
				},
			},
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("Get", mock.Anything, mock.Anything).Return(tc.wantRet, nil)
				tc.mockStore = mockStore
			},
		},
	}

	for _, tc := range cases {
		t.Run(tc.caseDesc, func(t *testing.T) {
			tc.mockFunc(tc)
			h := Handler{tc.mockStore}
			ctx := droplet.NewContext()
			ctx.SetInput(tc.giveInput)
			ret, err := h.Get(ctx)
			assert.Equal(t, err, tc.wantErr)
			if err == nil {
				assert.Equal(t, ret, tc.wantRet)
			}
		})
	}
}

func TestSystem_Post(t *testing.T) {
	t.Parallel()
	type testCase struct {
		caseDesc  string
		giveInput *entity.SystemConfig
		wantErr   error
		wantRet   interface{}
		mockStore store.Interface
		mockFunc  func(tc *testCase)
	}

	systemConfig := entity.SystemConfig{
		ConfigName: "grafana",
		Payload: map[string]interface{}{
			"url": "http://127.0.0.1:3000",
		},
	}

	cases := []*testCase{
		{
			caseDesc:  "create system config error",
			giveInput: &systemConfig,
			wantErr:   errors.New("mock error"),
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("Create", mock.Anything, mock.Anything).Return(nil, tc.wantErr)
				tc.mockStore = mockStore
			},
		},
		{
			caseDesc:  "create system config success",
			giveInput: &systemConfig,
			wantErr:   nil,
			wantRet: entity.SystemConfig{
				ConfigName: "grafana",
				Payload: map[string]interface{}{
					"url": "http://127.0.0.1:3000",
				},
			},
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("Create", mock.Anything, mock.Anything).Return(tc.wantRet, nil)
				tc.mockStore = mockStore
			},
		},
	}

	for _, tc := range cases {
		t.Run(tc.caseDesc, func(t *testing.T) {
			tc.mockFunc(tc)
			h := Handler{tc.mockStore}
			ctx := droplet.NewContext()
			ctx.SetInput(tc.giveInput)
			ret, err := h.Post(ctx)
			assert.Equal(t, err, tc.wantErr)
			if err == nil {
				assert.Equal(t, ret, tc.wantRet)
			}
		})
	}
}

func TestSystem_Put(t *testing.T) {
	t.Parallel()
	type testCase struct {
		caseDesc  string
		giveInput *entity.SystemConfig
		wantErr   error
		wantRet   interface{}
		mockStore store.Interface
		mockFunc  func(tc *testCase)
	}

	systemConfig := entity.SystemConfig{
		ConfigName: "grafana",
		Payload: map[string]interface{}{
			"url": "http://127.0.0.1:3000",
		},
	}

	cases := []*testCase{
		{
			caseDesc:  "update system config error",
			giveInput: &systemConfig,
			wantErr:   errors.New("mock error"),
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("Update", mock.Anything, mock.Anything, mock.Anything).Return(nil, tc.wantErr)
				tc.mockStore = mockStore
			},
		},
		{
			caseDesc:  "update system config success",
			giveInput: &systemConfig,
			wantErr:   nil,
			wantRet: entity.SystemConfig{
				ConfigName: "grafana",
				Payload: map[string]interface{}{
					"url": "http://127.0.0.1:3000",
				},
			},
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("Update", mock.Anything, mock.Anything, mock.Anything).Return(tc.wantRet, nil)
				tc.mockStore = mockStore
			},
		},
	}

	for _, tc := range cases {
		t.Run(tc.caseDesc, func(t *testing.T) {
			tc.mockFunc(tc)
			h := Handler{tc.mockStore}
			ctx := droplet.NewContext()
			ctx.SetInput(tc.giveInput)
			ret, err := h.Put(ctx)
			assert.Equal(t, err, tc.wantErr)
			if err == nil {
				assert.Equal(t, ret, tc.wantRet)
			}
		})
	}
}

func TestSystem_Delete(t *testing.T) {
	t.Parallel()
	type testCase struct {
		caseDesc  string
		giveInput *DeleteInput
		wantErr   error
		wantRet   interface{}
		mockStore store.Interface
		mockFunc  func(tc *testCase)
	}

	cases := []*testCase{
		{
			caseDesc:  "delete system config error",
			giveInput: &DeleteInput{ConfigName: "grafana"},
			wantErr:   errors.New("mock error"),
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("BatchDelete", mock.Anything, mock.Anything).Return(tc.wantErr)
				tc.mockStore = mockStore
			},
		},
		{
			caseDesc:  "delete system config success",
			giveInput: &DeleteInput{ConfigName: "grafana"},
			wantErr:   nil,
			mockFunc: func(tc *testCase) {
				mockStore := &store.MockInterface{}
				mockStore.On("BatchDelete", mock.Anything, mock.Anything).Return(tc.wantRet)
				tc.mockStore = mockStore
			},
		},
	}

	for _, tc := range cases {
		t.Run(tc.caseDesc, func(t *testing.T) {
			tc.mockFunc(tc)
			h := Handler{tc.mockStore}
			ctx := droplet.NewContext()
			ctx.SetInput(tc.giveInput)
			ret, err := h.Delete(ctx)
			assert.Equal(t, err, tc.wantErr)
			if err == nil {
				assert.Equal(t, ret, tc.wantRet)
			}
		})
	}
}
